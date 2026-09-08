package com.watanuki.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.app.WatanukiApp
import com.watanuki.sources.AnimeSources
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

/** Which sources feed the Home screen. A curated set is on by default. */
object SourcePrefs {
	private const val FILE = "sources"
	private const val KEY = "enabled"

	/** Folder names in upstream/src/es; the ones a Spanish-speaking user expects to see first. */
	private val defaultEnabled = listOf("animeav1", "animeflv", "jkanime", "monoschinos", "latanime", "tioanimeh", "animefenix", "animeyt")

	private fun prefs() = WatanukiApp.instance.getSharedPreferences(FILE, Context.MODE_PRIVATE)

	fun enabledIds(): Set<Long> {
		val stored = prefs().getStringSet(KEY, null)
		if (stored != null) return stored.mapNotNull { it.toLongOrNull() }.toSet()
		val all = AnimeSources.all
		val byFolder = all.filter { src -> defaultEnabled.any { key -> src.entry.className.contains(".$key.") } && !src.isNsfw }
		return (byFolder.ifEmpty { all.filterNot { it.isNsfw }.take(6) }).map { it.id }.toSet()
	}

	fun setEnabled(id: Long, enabled: Boolean) {
		val current = enabledIds().toMutableSet()
		if (enabled) current += id else current -= id
		prefs().edit().putStringSet(KEY, current.map { it.toString() }.toSet()).apply()
	}
}

data class FeedAnime(val anime: SAnime, val source: LoadedSource)

data class FeedRow(val title: String, val kicker: String?, val items: List<FeedAnime>, val source: LoadedSource? = null)

data class HomeState(
	val recommended: List<FeedAnime> = emptyList(),
	val latest: List<FeedAnime> = emptyList(),
	val rows: List<FeedRow> = emptyList(),
	val isLoading: Boolean = false,
	val loadedSources: Int = 0,
	val enabledSources: Int = 0,
	val error: String? = null,
)

/**
 * Home feed: for every enabled source, page 1 of popular and latest, mixed into
 * "Para ti" (random picks), "Recientes" (interleaved) and one "Populares en X" row per source.
 */
class HomeViewModel : ViewModel() {

	val state = MutableStateFlow(HomeState())
	private var job: Job? = null
	private var seed = Random.nextInt()

	fun load(force: Boolean = false) {
		if (!force && (state.value.rows.isNotEmpty() || job?.isActive == true)) return
		job?.cancel()
		if (force) seed = Random.nextInt()
		job = viewModelScope.launch {
			state.update { it.copy(isLoading = true, error = null, loadedSources = 0) }
			val enabled = withContext(Dispatchers.Default) {
				val ids = SourcePrefs.enabledIds()
				AnimeSources.all.filter { it.id in ids && (AppPrefs.showAdult || !it.isNsfw) }
			}
			state.update { it.copy(enabledSources = enabled.size) }
			if (enabled.isEmpty()) {
				state.update { it.copy(isLoading = false, error = "No hay fuentes activas") }
				return@launch
			}
			val random = Random(seed)
			val results = withContext(Dispatchers.IO) {
				coroutineScope {
					enabled.map { src ->
						async {
							val popular = withTimeoutOrNull(SOURCE_TIMEOUT) {
								runCatching { src.source.getPopularAnime(1).animes }.getOrNull()
							}.orEmpty().filterNot { !AppPrefs.showAdult && AppPrefs.isAdult(it, src) }
							val latest = if (src.source.supportsLatest) {
								withTimeoutOrNull(SOURCE_TIMEOUT) {
									runCatching { src.source.getLatestUpdates(1).animes }.getOrNull()
								}.orEmpty()
							} else {
								emptyList()
							}.filterNot { !AppPrefs.showAdult && AppPrefs.isAdult(it, src) }
							state.update { it.copy(loadedSources = it.loadedSources + 1) }
							Triple(src, popular, latest)
						}
					}.map { it.await() }
				}
			}
			val rows = results.filter { it.second.isNotEmpty() }.map { (src, popular, _) ->
				FeedRow(title = "Populares en ${src.name}", kicker = "人気", items = popular.take(20).map { FeedAnime(it, src) }, source = src)
			}
			val latest = interleave(results.map { (src, _, latest) -> latest.map { FeedAnime(it, src) } }).take(30)
			val recommended = results.flatMap { (src, popular, _) -> popular.shuffled(random).take(4).map { FeedAnime(it, src) } }
				.shuffled(random)
				.distinctBy { it.anime.title.lowercase() }
				.take(12)
			state.update {
				it.copy(
					recommended = recommended,
					latest = latest,
					rows = rows,
					isLoading = false,
					error = if (rows.isEmpty() && latest.isEmpty()) "Ninguna fuente respondió. Revisa la conexión." else null,
				)
			}
		}
	}

	private fun <T> interleave(lists: List<List<T>>): List<T> {
		val out = ArrayList<T>()
		val iterators = lists.map { it.iterator() }
		while (iterators.any { it.hasNext() }) {
			for (it in iterators) if (it.hasNext()) out += it.next()
		}
		return out
	}

	companion object {
		private const val SOURCE_TIMEOUT = 15_000L
	}
}
