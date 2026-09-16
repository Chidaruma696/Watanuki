package com.watanuki.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.app.catalog.EpisodeMerge
import com.watanuki.app.catalog.CatalogAnime
import com.watanuki.app.catalog.MergedEpisode
import com.watanuki.app.catalog.SourceMatch
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeasonState(
	val season: CatalogAnime,
	val episodes: List<MergedEpisode> = emptyList(),
	/** Names of the sources that contributed, for the "N fuentes" line only. */
	val sourceCount: Int = 0,
	val isLoading: Boolean = true,
	val error: String? = null,
	/** Episode whose servers are being resolved, and the result once ready. */
	val resolving: MergedEpisode? = null,
	val videos: List<Video>? = null,
	val videoError: String? = null,
)

/** One MAL season: episodes merged from every source that has it, and one-tap playback. */
class SeasonViewModel : ViewModel() {

	val state = MutableStateFlow<SeasonState?>(null)
	private var job: Job? = null
	private var playJob: Job? = null
	private val cache = HashMap<String, List<Video>>()

	fun open(season: CatalogAnime, hits: List<SourceMatch.Hit>) {
		if (state.value?.season?.id == season.id) return
		job?.cancel()
		state.value = SeasonState(season)
		job = viewModelScope.launch {
			try {
				val lists = EpisodeMerge.gather(hits)
				val merged = EpisodeMerge.merge(lists)
				state.update { it?.copy(episodes = merged, sourceCount = lists.size, isLoading = false, error = if (merged.isEmpty()) "Sin episodios en tus fuentes" else null) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				state.update { it?.copy(isLoading = false, error = e.message ?: e.javaClass.simpleName) }
			}
		}
	}

	/** Resolves every server of the episode; the screen plays the list as soon as it is ready. */
	fun play(episode: MergedEpisode) {
		playJob?.cancel()
		state.update { it?.copy(resolving = episode, videos = null, videoError = null) }
		cache[episode.key]?.let { cached ->
			state.update { it?.copy(videos = cached) }
			return
		}
		playJob = viewModelScope.launch {
			try {
				val videos = EpisodeMerge.videosFor(episode)
				if (state.value?.resolving?.key != episode.key) return@launch
				if (videos.isNotEmpty()) cache[episode.key] = videos
				state.update { it?.copy(videos = videos, videoError = if (videos.isEmpty()) "Sin servidores disponibles" else null) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				state.update { it?.copy(videoError = e.message ?: e.javaClass.simpleName) }
			}
		}
	}

	fun consumePlay() {
		state.update { it?.copy(resolving = null, videos = null, videoError = null) }
	}

	fun dismiss() {
		playJob?.cancel()
		consumePlay()
	}
}
