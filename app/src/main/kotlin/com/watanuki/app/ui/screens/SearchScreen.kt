package com.watanuki.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watanuki.app.R
import com.watanuki.app.ui.FeedAnime
import com.watanuki.app.ui.GlobalSearchViewModel
import com.watanuki.app.ui.SearchScope
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiLinearProgress
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

/**
 * Global search: one query, every source at once, one row of covers per source that answered.
 * "See more" opens that source's own catalogue with the same query.
 */
@Composable
fun GlobalSearchScreen(onOpen: (FeedAnime) -> Unit, onOpenSource: (LoadedSource, String) -> Unit, onBack: () -> Unit) {
	val vm = viewModel<GlobalSearchViewModel>()
	val state by vm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	var query by rememberSaveable { mutableStateOf(state.query) }
	val submit = { if (query.isNotBlank()) vm.search(query) }

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = stringResource(R.string.search),
				subtitle = stringResource(R.string.global_search_kicker),
				leading = { KomiButton(onClick = { vm.cancel(); onBack() }, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
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
			Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
				KomiSegmented(
					selected = state.scope,
					items = listOf(
						KomiSegmentedItem(SearchScope.ENABLED, stringResource(R.string.search_scope_enabled)),
						KomiSegmentedItem(SearchScope.ALL, stringResource(R.string.search_scope_all)),
					),
					onSelect = { vm.setScope(it) },
					height = 36.dp,
				)
			}
			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 32.dp),
				verticalArrangement = Arrangement.spacedBy(10.dp),
			) {
				if (state.isSearching) {
					item(key = "progress") {
						Column(Modifier.padding(horizontal = 16.dp)) {
							KomiLinearProgress(modifier = Modifier.fillMaxWidth())
							KomiText(
								text = stringResource(R.string.search_progress, state.finishedSources, state.totalSources),
								role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false,
								modifier = Modifier.padding(top = 4.dp),
							)
						}
					}
				}
				state.sections.forEach { section ->
					item(key = "head_${section.source.id}") {
						KomiSectionHead(
							label = section.source.name,
							kicker = stringResource(R.string.search_hits, section.items.size),
							modifier = Modifier.padding(horizontal = 16.dp),
							action = {
								KomiButton(
									onClick = { onOpenSource(section.source, state.query) },
									label = stringResource(R.string.see_more), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Text,
								)
							},
						)
					}
					item(key = "row_${section.source.id}") { CoverRow(items = section.items, onOpen = onOpen, showSource = false) }
				}
				when {
					state.query.isNotBlank() && !state.isSearching && state.totalSources == 0 -> item(key = "no_sources") {
						KomiText(text = stringResource(R.string.search_no_sources), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
					}
					state.isEmpty -> item(key = "empty") {
						KomiText(text = stringResource(R.string.nothing_found), role = KomiTextRole.Title, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
					}
					state.query.isBlank() -> item(key = "hint") {
						KomiText(text = stringResource(R.string.global_search_empty), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
					}
				}
			}
		}
	}
}
