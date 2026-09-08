package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

/** A basic interface for creating a source. It could be an online source, a local source, etc. */
interface AnimeSource {

	/** Id for the source. Must be unique. */
	val id: Long

	/** Name of the source. */
	val name: String

	suspend fun getAnimeDetails(anime: SAnime): SAnime {
		@Suppress("DEPRECATION")
		return fetchAnimeDetails(anime).awaitSingle()
	}

	suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
		@Suppress("DEPRECATION")
		return fetchEpisodeList(anime).awaitSingle()
	}

	suspend fun getVideoList(episode: SEpisode): List<Video> {
		@Suppress("DEPRECATION")
		return fetchVideoList(episode).awaitSingle()
	}

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getAnimeDetails"))
	fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> = throw UnsupportedOperationException("Not used")

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getEpisodeList"))
	fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> = throw UnsupportedOperationException("Not used")

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getVideoList"))
	fun fetchVideoList(episode: SEpisode): Observable<List<Video>> = throw UnsupportedOperationException("Not used")
}
