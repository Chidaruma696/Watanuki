package com.watanuki.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.sources.AnimeSources
import com.watanuki.sources.LoadedSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Which sources a global search covers. */
enum class SearchScope { ENABLED, ALL }

/** Results of one source for the current query; only sources with at least one hit are kept. */
data class SearchSection(val source: LoadedSource, val items: List<FeedAnime>)

data class SearchState(
	val query: String = "",
	val scope: SearchScope = SearchScope.ENABLED,
	val sections: List<SearchSection> = emptyList(),
	val isSearching: Boolean = false,
	val finishedSources: Int = 0,
	val totalSources: Int = 0,
) {
	/** True once a search ran to completion (or was cut short) without a single hit. */
	val isEmpty: Boolean get() = query.isNotBlank() && !isSearching && totalSources > 0 && sections.isEmpty()
}

/**
 * Global search: the same query sent to every source at once (at most [PARALLEL] in flight,
 * [SOURCE_TIMEOUT] each). Sections appear as sources answer, ordered like the source list;
 * a source that fails, times out or returns nothing is simply left out.
 */
class GlobalSearchViewModel : ViewModel() {

	val state = MutableStateFlow(SearchState())
	private var job: Job? = null

	fun setScope(scope: SearchScope) {
		if (state.value.scope == scope) return
		state.update { it.copy(scope = scope) }
		val q = state.value.query
		if (q.isNotBlank()) search(q, force = true)
	}

	fun search(rawQuery: String, force: Boolean = false) {
		val query = rawQuery.trim()
		if (query.isBlank()) { cancel(); state.update { it.copy(query = "", sections = emptyList(), totalSources = 0, finishedSources = 0) }; return }
		if (!force && query == state.value.query && state.value.isSearching) return
		job?.cancel()
		state.update { it.copy(query = query, sections = emptyList(), isSearching = true, finishedSources = 0, totalSources = 0) }
		job = viewModelScope.launch {
			val sources = withContext(Dispatchers.Default) {
				val ids = if (state.value.scope == SearchScope.ENABLED) SourcePrefs.enabledIds() else null
				AnimeSources.all
					.filter { (ids == null || it.id in ids) && (AppPrefs.showAdult || !it.isNsfw) }
					.sortedBy { it.name.lowercase() }
			}
			state.update { it.copy(totalSources = sources.size) }
			if (sources.isEmpty()) {
				state.update { it.copy(isSearching = false) }
				return@launch
			}
			val order = sources.withIndex().associate { (i, src) -> src.id to i }
			val semaphore = Semaphore(PARALLEL)
			val workers = sources.map { src ->
				launch(Dispatchers.IO) {
					val hits = semaphore.withPermit { searchOne(src, query) }
					state.update { s ->
						val sections = if (hits.isEmpty()) s.sections else (s.sections + SearchSection(src, hits)).sortedBy { order[it.source.id] ?: Int.MAX_VALUE }
						s.copy(sections = sections, finishedSources = s.finishedSources + 1)
					}
				}
			}
			workers.forEach { it.join() }
			state.update { it.copy(isSearching = false) }
		}
	}

	/** Stops whatever is in flight; the results gathered so far stay on screen. */
	fun cancel() {
		job?.cancel()
		job = null
		state.update { it.copy(isSearching = false) }
	}

	private suspend fun searchOne(src: LoadedSource, query: String): List<FeedAnime> = try {
		withTimeoutOrNull(SOURCE_TIMEOUT) {
			src.source.getSearchAnime(1, query, src.source.getFilterList()).animes
		}.orEmpty()
			.filterNot { !AppPrefs.showAdult && AppPrefs.isAdult(it, src) }
			.distinctBy { it.url }
			.take(MAX_PER_SOURCE)
			.map { FeedAnime(it, src) }
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		emptyList()
	}

	companion object {
		private const val PARALLEL = 6
		private const val SOURCE_TIMEOUT = 15_000L
		private const val MAX_PER_SOURCE = 20
	}
}
