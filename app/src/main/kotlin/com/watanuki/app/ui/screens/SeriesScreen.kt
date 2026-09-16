package com.watanuki.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watanuki.app.R
import com.watanuki.app.catalog.Backend
import com.watanuki.app.catalog.CatalogAnime
import com.watanuki.app.catalog.CatalogSeries
import com.watanuki.app.catalog.SourceMatch
import com.watanuki.app.ui.SeriesViewModel
import com.watanuki.app.ui.komi.KomiBadge
import com.watanuki.app.ui.komi.KomiBadgeTone
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiChip
import com.watanuki.app.ui.komi.KomiChipKind
import com.watanuki.app.ui.komi.KomiLinearProgress
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSurface
import com.watanuki.app.ui.komi.KomiSurfaceElevation
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.LocalPersonality

/** One line of facts about a MAL entry: "TV · 2011 · 13 ep". */
internal fun CatalogAnime.meta(episodesWord: String): String =
	listOfNotNull(type, year?.toString(), episodes?.let { "$it $episodesWord" }).joinToString(" · ")

/**
 * A series as MAL sees it: the show's card and its seasons in order. Each season is looked up
 * in the sources behind the scenes; tapping one opens it where it was found.
 */
@Composable
fun SeriesScreen(series: CatalogSeries, onOpenSeason: (CatalogAnime, List<SourceMatch.Hit>) -> Unit, onSearchBySource: (String) -> Unit, onBack: () -> Unit) {
	val vm = viewModel<SeriesViewModel>()
	LaunchedEffect(series.id) { vm.open(series) }
	val state by vm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	val current = state?.series ?: series
	val main = current.main
	val episodesWord = stringResource(R.string.episodes).lowercase()

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = current.title,
				subtitle = if (current.backend == Backend.ANILIST) "AniList" else "MyAnimeList",
				leading = { KomiButton(onClick = onBack, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
			)
		},
	) { padding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item(key = "card") {
				KomiSurface(modifier = Modifier.fillMaxWidth(), elevation = KomiSurfaceElevation.Card, contentPadding = PaddingValues(13.dp)) {
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						Cover(url = main.cover, referer = null, modifier = Modifier.width(110.dp))
						Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							KomiText(text = main.title, role = KomiTextRole.Title, maxLines = 3, overflow = TextOverflow.Ellipsis)
							main.titleEnglish?.takeIf { it.isNotBlank() && it != main.title }?.let {
								KomiText(text = it, role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, maxLines = 2, overflow = TextOverflow.Ellipsis)
							}
							Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
								KomiChip(label = stringResource(R.string.seasons_count, current.seasons.size), kind = KomiChipKind.Filter, selected = true, small = true, tilt = false)
								main.score?.let { KomiChip(label = "★ $it", kind = KomiChipKind.Info, small = true, tilt = false) }
							}
							KomiText(text = main.meta(episodesWord), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false)
						}
					}
				}
			}
			main.synopsis?.takeIf { it.isNotBlank() }?.let { desc ->
				item(key = "synopsis") { KomiText(text = desc, role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false) }
			}
			item(key = "head") {
				KomiSectionHead(label = stringResource(R.string.seasons), kicker = "期")
			}
			if (state?.expanding == true) {
				item(key = "expanding") {
					Column {
						KomiLinearProgress(modifier = Modifier.fillMaxWidth())
						KomiText(text = stringResource(R.string.seasons_loading), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, modifier = Modifier.padding(top = 4.dp))
					}
				}
			}
			item(key = "seasons") {
				KomiListContainer {
					current.seasons.forEachIndexed { index, season ->
						val hits = state?.matches?.get(season.id)
						val status = when {
							hits == null -> stringResource(R.string.season_looking)
							hits.isEmpty() -> stringResource(R.string.season_missing)
							else -> stringResource(R.string.season_found, hits.size)
						}
						KomiListRow(
							title = "${index + 1}. ${season.title}",
							subtitle = listOf(season.meta(episodesWord), status).filter { it.isNotBlank() }.joinToString(" · "),
							onClick = { if (!hits.isNullOrEmpty()) onOpenSeason(season, hits) else if (hits != null) onSearchBySource(season.title) },
							showDivider = index < current.seasons.lastIndex,
							trailing = {
								when {
									hits == null -> KomiBadge(text = "…", tone = KomiBadgeTone.Neutral)
									hits.isEmpty() -> KomiBadge(text = "✗", tone = KomiBadgeTone.Alert, tilt = true)
									else -> KomiText(text = "▶", role = KomiTextRole.Label, color = colors.primary)
								}
							},
						)
					}
				}
			}
		}
	}
}
