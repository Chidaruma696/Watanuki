package com.watanuki.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watanuki.app.catalog.CatalogSeries
import com.watanuki.app.catalog.SeriesGrouper
import com.watanuki.app.catalog.SourceMatch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesState(
	val series: CatalogSeries,
	/** True while the prequel/sequel chain is still being walked. */
	val expanding: Boolean = true,
	/** catalogue id -> where that season was found; absent while still looking, empty when nowhere. */
	val matches: Map<Int, List<SourceMatch.Hit>> = emptyMap(),
)

/**
 * One series: first the full season list from MAL, then each season looked up in the sources.
 * Seasons are matched one at a time so the list fills in top to bottom.
 */
class SeriesViewModel : ViewModel() {

	val state = MutableStateFlow<SeriesState?>(null)
	private var job: Job? = null

	fun open(series: CatalogSeries) {
		if (state.value?.series?.id == series.id) return
		job?.cancel()
		state.value = SeriesState(series)
		job = viewModelScope.launch {
			val expanded = try { SeriesGrouper.expand(series) } catch (e: CancellationException) { throw e } catch (e: Throwable) { series }
			state.update { it?.copy(series = expanded, expanding = false) }
			for (season in expanded.seasons) {
				val hits = try { SourceMatch.find(season) } catch (e: CancellationException) { throw e } catch (e: Throwable) { emptyList() }
				state.update { s -> s?.copy(matches = s.matches + (season.id to hits)) }
			}
		}
	}

	override fun onCleared() {
		job?.cancel()
	}
}
