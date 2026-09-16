package com.watanuki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watanuki.app.R
import com.watanuki.app.catalog.CatalogSeries
import com.watanuki.app.ui.CatalogSearchViewModel
import com.watanuki.app.ui.FeedAnime
import com.watanuki.app.ui.GlobalSearchViewModel
import com.watanuki.app.ui.SearchScope
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiLinearProgress
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSegmented
import com.watanuki.app.ui.komi.KomiSegmentedItem
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextField
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.LocalPersonality
import com.watanuki.sources.LoadedSource

/** Catalogue asks MyAnimeList and groups seasons; "by source" asks every streaming site directly. */
enum class SearchMode { CATALOG, SOURCES }

/**
 * Global search. The catalogue mode is the default: one card per series, whatever the sites
 * call it. The by-source mode is the old one-row-per-site view, kept for what MAL misses.
 */
@Composable
fun GlobalSearchScreen(
	initialQuery: String,
	bySource: Boolean,
	onOpen: (FeedAnime) -> Unit,
	onOpenSource: (LoadedSource, String) -> Unit,
	onOpenSeries: (CatalogSeries) -> Unit,
	onBack: () -> Unit,
) {
	val sourcesVm = viewModel<GlobalSearchViewModel>()
	val catalogVm = viewModel<CatalogSearchViewModel>()
	val sourcesState by sourcesVm.state.collectAsState()
	val catalogState by catalogVm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	var mode by rememberSaveable { mutableStateOf(if (bySource) SearchMode.SOURCES else SearchMode.CATALOG) }
	var query by rememberSaveable { mutableStateOf(initialQuery.ifBlank { if (bySource) sourcesState.query else catalogState.query }) }
	val submit = {
		if (query.isNotBlank()) {
			if (mode == SearchMode.CATALOG) catalogVm.search(query) else sourcesVm.search(query)
		}
	}
	LaunchedEffect(Unit) { if (initialQuery.isNotBlank()) submit() }
	val episodesWord = stringResource(R.string.episodes).lowercase()

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = stringResource(R.string.search),
				subtitle = if (mode == SearchMode.CATALOG) stringResource(R.string.catalog_kicker) else stringResource(R.string.global_search_kicker),
				leading = { KomiButton(onClick = { sourcesVm.cancel(); catalogVm.cancel(); onBack() }, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
			)
		},
	) { padding ->
		Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
			Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
				KomiTextField(
					value = query,
					onValueChange = { query = it },
					modifier = Modifier.weight(1f),
					placeholder = stringResource(R.string.global_search_hint),
					onCommit = submit,
				)
				KomiButton(onClick = submit, label = stringResource(R.string.search), size = KomiButtonSize.Sm, enabled = query.isNotBlank())
			}
			Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
				KomiSegmented(
					selected = mode,
					items = listOf(
						KomiSegmentedItem(SearchMode.CATALOG, stringResource(R.string.search_mode_catalog)),
						KomiSegmentedItem(SearchMode.SOURCES, stringResource(R.string.search_mode_sources)),
					),
					onSelect = { mode = it; submit() },
					height = 36.dp,
				)
				if (mode == SearchMode.SOURCES) {
					KomiSegmented(
						selected = sourcesState.scope,
						items = listOf(
							KomiSegmentedItem(SearchScope.ENABLED, stringResource(R.string.search_scope_enabled)),
							KomiSegmentedItem(SearchScope.ALL, stringResource(R.string.search_scope_all)),
						),
						onSelect = { sourcesVm.setScope(it) },
						height = 36.dp,
					)
				}
			}
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 32.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				if (mode == SearchMode.CATALOG) {
					if (catalogState.isSearching) {
						item(key = "progress") {
							Column(Modifier.padding(horizontal = 16.dp)) {
								KomiLinearProgress(modifier = Modifier.fillMaxWidth())
								KomiText(text = stringResource(R.string.catalog_searching), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, modifier = Modifier.padding(top = 4.dp))
							}
						}
					}
					if (catalogState.series.isNotEmpty()) {
						item(key = "series") {
							KomiListContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
								catalogState.series.forEachIndexed { index, series ->
									SeriesRow(series, episodesWord, showDivider = index < catalogState.series.lastIndex, onClick = { onOpenSeries(series) })
								}
							}
						}
					}
					when {
						catalogState.error != null -> item(key = "error") {
							Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
								KomiText(text = stringResource(R.string.catalog_error, catalogState.error.orEmpty()), role = KomiTextRole.Body, color = colors.error, uppercase = false, textAlign = TextAlign.Center)
								KomiButton(onClick = { mode = SearchMode.SOURCES; submit() }, label = stringResource(R.string.search_mode_sources), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
							}
						}
						catalogState.isEmpty -> item(key = "empty") {
							KomiText(text = stringResource(R.string.nothing_found), role = KomiTextRole.Title, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
						}
						catalogState.query.isBlank() -> item(key = "hint") {
							KomiText(text = stringResource(R.string.catalog_empty), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
						}
					}
				} else {
					if (sourcesState.isSearching) {
						item(key = "progress") {
							Column(Modifier.padding(horizontal = 16.dp)) {
								KomiLinearProgress(modifier = Modifier.fillMaxWidth())
								KomiText(
									text = stringResource(R.string.search_progress, sourcesState.finishedSources, sourcesState.totalSources),
									role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false,
									modifier = Modifier.padding(top = 4.dp),
								)
							}
						}
					}
					sourcesState.sections.forEach { section ->
						item(key = "head_${section.source.id}") {
							KomiSectionHead(
								label = section.source.name,
								kicker = stringResource(R.string.search_hits, section.items.size),
								modifier = Modifier.padding(horizontal = 16.dp),
								action = {
									KomiButton(
										onClick = { onOpenSource(section.source, sourcesState.query) },
										label = stringResource(R.string.see_more), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Text,
									)
								},
							)
						}
						item(key = "row_${section.source.id}") { CoverRow(items = section.items, onOpen = onOpen, showSource = false) }
					}
					when {
						sourcesState.query.isNotBlank() && !sourcesState.isSearching && sourcesState.totalSources == 0 -> item(key = "no_sources") {
							KomiText(text = stringResource(R.string.search_no_sources), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
						}
						sourcesState.isEmpty -> item(key = "empty") {
							KomiText(text = stringResource(R.string.nothing_found), role = KomiTextRole.Title, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
						}
						sourcesState.query.isBlank() -> item(key = "hint") {
							KomiText(text = stringResource(R.string.global_search_empty), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
						}
					}
				}
			}
		}
	}
}

/** One series in the catalogue results: cover, title, and how much of it there is. */
@Composable
private fun SeriesRow(series: CatalogSeries, episodesWord: String, showDivider: Boolean, onClick: () -> Unit) {
	val colors = LocalPersonality.current.colors
	val main = series.main
	Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
		Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
			Cover(url = main.cover, referer = null, modifier = Modifier.width(64.dp))
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
				KomiText(text = main.title, role = KomiTextRole.Label, uppercase = false, maxLines = 2, overflow = TextOverflow.Ellipsis)
				main.titleEnglish?.takeIf { it.isNotBlank() && it != main.title }?.let {
					KomiText(text = it, role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, maxLines = 1, overflow = TextOverflow.Ellipsis)
				}
				val totalEpisodes = series.seasons.sumOf { it.episodes ?: 0 }
				val facts = listOfNotNull(
					main.type, main.year?.toString(),
					stringResource(R.string.seasons_count, series.seasons.size),
					totalEpisodes.takeIf { it > 0 }?.let { "$it $episodesWord" },
				)
				KomiText(text = facts.joinToString(" · "), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false)
			}
		}
		if (showDivider) {
			Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
		}
	}
}
