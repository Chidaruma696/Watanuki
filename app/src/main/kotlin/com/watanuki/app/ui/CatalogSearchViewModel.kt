package com.watanuki.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.app.catalog.Catalog
import com.watanuki.app.catalog.CatalogSeries
import com.watanuki.app.catalog.SeriesGrouper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogState(
	val query: String = "",
	val series: List<CatalogSeries> = emptyList(),
	val isSearching: Boolean = false,
	val error: String? = null,
) {
	val isEmpty: Boolean get() = query.isNotBlank() && !isSearching && error == null && series.isEmpty()
}

/** Catalogue search: the title goes to MyAnimeList and the hits come back collapsed into series. */
class CatalogSearchViewModel : ViewModel() {

	val state = MutableStateFlow(CatalogState())
	private var job: Job? = null

	fun search(rawQuery: String) {
		val query = rawQuery.trim()
		if (query.isBlank()) { cancel(); state.update { CatalogState() }; return }
		if (query == state.value.query && (state.value.isSearching || state.value.series.isNotEmpty())) return
		job?.cancel()
		state.update { it.copy(query = query, series = emptyList(), isSearching = true, error = null) }
		job = viewModelScope.launch {
			try {
				val hits = Catalog.search(query, sfw = !AppPrefs.showAdult)
				val grouped = SeriesGrouper.group(hits) { partial -> state.update { it.copy(series = partial) } }
				state.update { it.copy(series = grouped, isSearching = false) }
			} catch (e: CancellationException) {
				throw e
			} catch (e: Throwable) {
				state.update { it.copy(isSearching = false, error = e.message ?: e.javaClass.simpleName) }
			}
		}
	}

	fun cancel() {
		job?.cancel()
		job = null
		state.update { it.copy(isSearching = false) }
	}
}
