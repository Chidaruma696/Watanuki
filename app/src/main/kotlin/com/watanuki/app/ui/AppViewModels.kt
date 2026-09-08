package com.watanuki.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.sources.AnimeSources
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

enum class BrowseMode { POPULAR, LATEST, SEARCH }

data class BrowseState(
	val items: List<SAnime> = emptyList(),
	val page: Int = 0,
	val hasNext: Boolean = true,
	val isLoading: Boolean = false,
	val error: String? = null,
	val mode: BrowseMode = BrowseMode.POPULAR,
	val query: String = "",
)

/** Catalogue of one source: popular / latest / search with page-by-page loading. */
class BrowseViewModel : ViewModel() {

	val state = MutableStateFlow(BrowseState())
	private var source: LoadedSource? = null
	private var job: Job? = null

	fun open(source: LoadedSource, mode: BrowseMode = BrowseMode.POPULAR, query: String = "") {
		if (this.source?.id == source.id && state.value.mode == mode && state.value.query == query && state.value.items.isNotEmpty()) return
		this.source = source
		job?.cancel()
		state.value = BrowseState(mode = mode, query = query)
		loadMore()
	}

	fun loadMore() {
		val src = source ?: return
		val s = state.value
		if (s.isLoading || !s.hasNext) return
		job = viewModelScope.launch {
			state.update { it.copy(isLoading = true, error = null) }
			val page = s.page + 1
			try {
				val result = withContext(Dispatchers.IO) {
					when (s.mode) {
						BrowseMode.POPULAR -> src.source.getPopularAnime(page)
						BrowseMode.LATEST -> src.source.getLatestUpdates(page)
						BrowseMode.SEARCH -> src.source.getSearchAnime(page, s.query, src.source.getFilterList())
					}
				}
				val visible = result.animes.filterNot { !AppPrefs.showAdult && AppPrefs.isAdult(it, src) }
				state.update {
					it.copy(
						items = (it.items + visible).distinctBy { a -> a.url },
						page = page,
						hasNext = result.hasNextPage,
						isLoading = false,
					)
				}
			} catch (e: Throwable) {
				state.update { it.copy(isLoading = false, error = e.message ?: e.javaClass.simpleName) }
			}
		}
	}
}

data class DetailsState(
	val anime: SAnime? = null,
	val episodes: List<SEpisode> = emptyList(),
	val isLoading: Boolean = false,
	val error: String? = null,
	val videos: List<Video>? = null,
	val videosFor: SEpisode? = null,
	val isLoadingVideos: Boolean = false,
	val videoError: String? = null,
	val autoPlay: Boolean = false,
	val autoPlayVideo: Video? = null,
)

/** Anime page: details + episode list, and video link resolution for one episode. */
class DetailsViewModel : ViewModel() {

	val state = MutableStateFlow(DetailsState())
	private var source: LoadedSource? = null

	fun open(source: LoadedSource, anime: SAnime) {
		if (this.source?.id == source.id && state.value.anime?.url == anime.url) return
		this.source = source
		state.value = DetailsState(anime = anime, isLoading = true)
		viewModelScope.launch {
			try {
				val (details, episodes) = withContext(Dispatchers.IO) {
					val d = runCatching { source.source.getAnimeDetails(anime) }.getOrDefault(anime).also { it.url = anime.url }
					val e = source.source.getEpisodeList(anime)
					d to e
				}
				state.update { it.copy(anime = details, episodes = episodes, isLoading = false) }
			} catch (e: Throwable) {
				state.update { it.copy(isLoading = false, error = e.message ?: e.javaClass.simpleName) }
			}
		}
	}

	private var videosJob: Job? = null
	private var requestToken = 0
	private val videoCache = HashMap<String, List<Video>>()

	/**
	 * Resolves the servers of an episode; with [autoPlay] the first usable one is played right away.
	 * Stale results (a request the user cancelled or superseded) are ignored, and cancellation is
	 * never shown as an error.
	 */
	fun loadVideos(episode: SEpisode, autoPlay: Boolean) {
		val src = source ?: return
		videosJob?.cancel()
		val token = ++requestToken
		state.update { it.copy(videos = null, videosFor = episode, isLoadingVideos = true, videoError = null, autoPlay = autoPlay, autoPlayVideo = null) }
		videoCache[episode.url]?.let { cached ->
			state.update { it.copy(videos = cached, isLoadingVideos = false, autoPlayVideo = if (autoPlay) cached.firstOrNull() else null) }
			return
		}
		videosJob = viewModelScope.launch {
			try {
				val videos = withTimeout(VIDEO_TIMEOUT) {
					withContext(Dispatchers.IO) { src.source.getVideoList(episode) }
				}
				val playable = videos.filter { !it.videoUrl.isNullOrBlank() || it.url.isNotBlank() }
				if (token != requestToken) return@launch
				if (playable.isNotEmpty()) videoCache[episode.url] = playable
				state.update {
					it.copy(
						videos = playable,
						isLoadingVideos = false,
						autoPlayVideo = if (autoPlay) playable.firstOrNull() else null,
						videoError = if (playable.isEmpty()) "Sin servidores disponibles" else null,
					)
				}
			} catch (e: kotlinx.coroutines.CancellationException) {
				throw e
			} catch (e: kotlinx.coroutines.TimeoutCancellationException) {
				if (token == requestToken) state.update { it.copy(isLoadingVideos = false, videoError = "timeout") }
			} catch (e: Throwable) {
				if (token == requestToken) state.update { it.copy(isLoadingVideos = false, videoError = e.message ?: e.javaClass.simpleName) }
			}
		}
	}

	/** Switch the open sheet from automatic to manual without re-resolving. */
	fun chooseManually() {
		val ep = state.value.videosFor ?: return
		if (state.value.videos != null) {
			state.update { it.copy(autoPlay = false, autoPlayVideo = null) }
		} else {
			loadVideos(ep, autoPlay = false)
		}
	}

	fun consumeAutoPlay() {
		state.update { it.copy(autoPlayVideo = null, videosFor = null, videos = null, autoPlay = false) }
	}

	fun dismissVideos() {
		requestToken++
		videosJob?.cancel()
		state.update { it.copy(videos = null, videosFor = null, isLoadingVideos = false, videoError = null, autoPlay = false, autoPlayVideo = null) }
	}

	companion object {
		private const val VIDEO_TIMEOUT = 90_000L
	}
}

/** All compiled sources, loaded once off the main thread. */
class SourcesViewModel : ViewModel() {
	val sources = MutableStateFlow<List<LoadedSource>?>(null)
	val failed = MutableStateFlow(0)

	init {
		viewModelScope.launch(Dispatchers.Default) {
			val all = AnimeSources.all.sortedBy { it.name.lowercase() }
			sources.value = all
			failed.value = AnimeSources.failed.size
		}
	}
}
