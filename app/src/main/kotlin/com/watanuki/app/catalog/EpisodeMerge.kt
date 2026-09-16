package com.watanuki.app.catalog

import com.watanuki.app.ui.VideoQuality
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Headers

/** One site's copy of an episode. */
data class EpisodeCopy(val source: LoadedSource, val anime: SAnime, val episode: SEpisode)

/** An episode as the viewer sees it: one number, however many sites carry it. Best copy first. */
data class MergedEpisode(val key: String, val number: Float, val name: String, val copies: List<EpisodeCopy>) {
	val primary: EpisodeCopy get() = copies.first()
}

/**
 * Episodes of one season gathered from every source that has it, matched by number so a site
 * that stops at episode 12 is completed by another. The copies behind each episode are what
 * playback and downloads walk when a server fails.
 */
object EpisodeMerge {

	/** Same key, same episode, whatever site it is on. */
	fun key(ep: SEpisode): String = when {
		ep.episode_number > 0f -> "n:" + (if (ep.episode_number % 1f == 0f) ep.episode_number.toInt().toString() else ep.episode_number.toString())
		else -> "t:" + SourceMatch.key(ep.name)
	}

	/** Episode lists of every matched copy, in parallel; a source that fails is just left out. */
	suspend fun gather(hits: List<SourceMatch.Hit>): List<Pair<EpisodeCopy, List<SEpisode>>> = withContext(Dispatchers.IO) {
		coroutineScope {
			hits.map { hit ->
				async {
					try {
						withTimeoutOrNull(LIST_TIMEOUT) {
							val episodes = hit.feed.source.source.getEpisodeList(hit.feed.anime)
							if (episodes.isEmpty()) null else EpisodeCopy(hit.feed.source, hit.feed.anime, episodes.first()) to episodes
						}
					} catch (e: CancellationException) {
						throw e
					} catch (e: Throwable) {
						null
					}
				}
			}.mapNotNull { it.await() }
		}
	}

	/** Merges the lists: the first (best-matched) source wins, the others fill gaps and back it up. Ascending. */
	fun merge(lists: List<Pair<EpisodeCopy, List<SEpisode>>>): List<MergedEpisode> {
		val byKey = LinkedHashMap<String, MutableList<EpisodeCopy>>()
		for ((owner, episodes) in lists) {
			for (ep in episodes) {
				byKey.getOrPut(key(ep)) { mutableListOf() } += EpisodeCopy(owner.source, owner.anime, ep)
			}
		}
		return byKey.map { (k, copies) ->
			val first = copies.first().episode
			MergedEpisode(k, first.episode_number, first.name, copies)
		}.sortedWith(compareBy({ if (it.number > 0f) 0 else 1 }, { it.number }, { it.name }))
	}

	/**
	 * Every server of every copy, best quality first, each with the Referer its site needs so
	 * the player can take the list as is.
	 */
	suspend fun videosFor(episode: MergedEpisode): List<Video> = withContext(Dispatchers.IO) {
		coroutineScope {
			episode.copies.map { copy ->
				async {
					try {
						withTimeoutOrNull(VIDEO_TIMEOUT) { copy.source.source.getVideoList(copy.episode) }
							.orEmpty()
							.filter { !it.videoUrl.isNullOrBlank() || it.url.isNotBlank() }
							.map { withReferer(it, copy.source) }
					} catch (e: CancellationException) {
						throw e
					} catch (e: Throwable) {
						emptyList()
					}
				}
			}.flatMap { it.await() }
		}.sortedWith(VideoQuality.comparator())
	}

	/** Adds the site's own Referer unless the extractor already set one (any spelling). */
	private fun withReferer(video: Video, source: LoadedSource): Video {
		val base = (source.source as? AnimeHttpSource)?.baseUrl ?: return video
		val existing = video.headers
		if (existing != null && existing.names().any { it.equals("Referer", true) }) return video
		val headers = (existing?.newBuilder() ?: Headers.Builder()).add("Referer", base).build()
		return video.copy(headers = headers)
	}

	private const val LIST_TIMEOUT = 30_000L
	private const val VIDEO_TIMEOUT = 45_000L
}
