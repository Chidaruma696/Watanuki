package com.watanuki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.watanuki.app.R
import com.watanuki.app.catalog.Backend
import com.watanuki.app.catalog.CatalogAnime
import com.watanuki.app.catalog.MergedEpisode
import com.watanuki.app.catalog.SourceMatch
import com.watanuki.app.download.DownloadItem
import com.watanuki.app.download.DownloadRepository
import com.watanuki.app.download.DownloadService
import com.watanuki.app.download.EpisodeCopyRef
import com.watanuki.app.download.humanSize
import com.watanuki.app.ui.SeasonViewModel
import com.watanuki.app.ui.komi.KomiBadge
import com.watanuki.app.ui.komi.KomiBadgeTone
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiChip
import com.watanuki.app.ui.komi.KomiChipKind
import com.watanuki.app.ui.komi.KomiCircularProgress
import com.watanuki.app.ui.komi.KomiLinearProgress
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSheet
import com.watanuki.app.ui.komi.KomiSurface
import com.watanuki.app.ui.komi.KomiSurfaceElevation
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.LocalPersonality
import eu.kanade.tachiyomi.animesource.model.Video

/** "Episodio 3", or the site's own name when it does not number them. */
private fun MergedEpisode.label(word: String): String =
	if (number > 0f) "$word ${if (number % 1f == 0f) number.toInt() else number}" else name

/** Copies beyond the first, so a download can fall back to another site. */
private fun MergedEpisode.alternates(): List<EpisodeCopyRef> =
	copies.drop(1).map { EpisodeCopyRef(it.source.id, it.episode.url, it.episode.name, it.episode.episode_number) }

/**
 * A season from the catalogue: MAL's card on top, then the episodes merged from every source
 * that has it, 1 → N. Tap plays the best server of any site; the sites themselves stay out of view.
 */
@Composable
fun SeasonScreen(season: CatalogAnime, hits: List<SourceMatch.Hit>, onPlay: (String, List<Video>) -> Unit, onBack: () -> Unit) {
	val vm = viewModel<SeasonViewModel>()
	LaunchedEffect(season.id) { vm.open(season, hits) }
	val state by vm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	val context = LocalContext.current
	val downloads by DownloadRepository.items.collectAsState()
	val episodeWord = stringResource(R.string.episode)
	val episodesWord = stringResource(R.string.episodes).lowercase()
	val episodes = state?.episodes.orEmpty()

	fun downloadOf(ep: MergedEpisode): DownloadItem? =
		downloads.firstOrNull { d -> ep.copies.any { c -> d.sourceId == c.source.id && d.episodeUrl == c.episode.url } }

	fun enqueue(ep: MergedEpisode): Boolean {
		val p = ep.primary
		return DownloadRepository.enqueueEpisode(p.source, p.anime, p.episode, ep.alternates()) != null
	}

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = season.title,
				subtitle = if (season.backend == Backend.ANILIST) "AniList" else "MyAnimeList",
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
						Cover(url = season.cover, referer = null, modifier = Modifier.width(110.dp))
						Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							KomiText(text = season.title, role = KomiTextRole.Title, maxLines = 3, overflow = TextOverflow.Ellipsis)
							Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
								season.status?.let { KomiChip(label = it, kind = KomiChipKind.Filter, selected = true, small = true, tilt = false) }
								season.score?.let { KomiChip(label = "★ $it", kind = KomiChipKind.Info, small = true, tilt = false) }
							}
							KomiText(text = season.meta(episodesWord), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false)
						}
					}
				}
			}
			season.synopsis?.takeIf { it.isNotBlank() }?.let { desc ->
				item(key = "synopsis") { KomiText(text = desc, role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false) }
			}
			item(key = "head") {
				KomiSectionHead(
					label = "${episodes.size} ${stringResource(R.string.episodes)}", kicker = "話",
					action = if (episodes.isNotEmpty()) {
						{
							KomiButton(
								onClick = {
									val queued = episodes.count { ep -> enqueue(ep) }
									if (queued > 0) DownloadService.start(context)
									android.widget.Toast.makeText(context, context.getString(R.string.download_season_queued, queued), android.widget.Toast.LENGTH_SHORT).show()
								},
								label = stringResource(R.string.download_season), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline,
							)
						}
					} else null,
				)
			}
			when {
				state?.isLoading != false -> item(key = "loading") {
					Column(Modifier.fillMaxWidth()) {
						KomiLinearProgress(modifier = Modifier.fillMaxWidth())
						KomiText(text = stringResource(R.string.season_gathering, hits.size), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, modifier = Modifier.padding(top = 4.dp))
					}
				}
				state?.error != null -> item(key = "error") {
					KomiText(text = state?.error.orEmpty(), role = KomiTextRole.Body, color = colors.error, uppercase = false, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
				}
				else -> item(key = "episodes") {
					KomiListContainer {
						episodes.forEachIndexed { index, ep ->
							val download = downloadOf(ep)
							val active = download?.takeIf { it.status == DownloadItem.STATUS_RUNNING || it.status == DownloadItem.STATUS_QUEUED }
							val progressText = when {
								active == null -> null
								active.status == DownloadItem.STATUS_QUEUED -> stringResource(R.string.download_queued)
								active.total > 0 -> "${active.bytes * 100 / active.total} % · ${humanSize(active.bytes)}"
								active.url.isBlank() -> stringResource(R.string.download_resolving)
								else -> humanSize(active.bytes)
							}
							KomiListRow(
								title = ep.label(episodeWord),
								subtitle = progressText ?: ep.name.takeIf { ep.number > 0f && !it.equals(ep.label(episodeWord), true) },
								onClick = { vm.play(ep) },
								showDivider = index < episodes.lastIndex && active == null,
								trailing = {
									Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
										when (download?.status) {
											DownloadItem.STATUS_DONE -> KomiBadge(text = "✓", tone = KomiBadgeTone.Neutral)
											DownloadItem.STATUS_RUNNING, DownloadItem.STATUS_QUEUED -> KomiBadge(text = "…", tone = KomiBadgeTone.Neutral)
											else -> KomiButton(
												onClick = {
													if (enqueue(ep)) {
														DownloadService.start(context)
														android.widget.Toast.makeText(context, R.string.download_started, android.widget.Toast.LENGTH_SHORT).show()
													}
												},
												label = "⤓", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline,
											)
										}
										KomiText(text = "▶", role = KomiTextRole.Label, color = colors.primary)
									}
								},
							)
							if (active != null) {
								KomiLinearProgress(
									modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp),
									progress = if (active.status == DownloadItem.STATUS_RUNNING && active.total > 0) ({ (active.bytes.toFloat() / active.total).coerceIn(0f, 1f) }) else null,
								)
								if (index < episodes.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outline))
							}
						}
					}
				}
			}
		}
	}

	// play as soon as the servers are known; the list is already best-first
	val resolving = state?.resolving
	val videos = state?.videos
	if (resolving != null && videos != null) {
		LaunchedEffect(resolving.key, videos) {
			vm.consumePlay()
			onPlay("${season.title} · ${resolving.label(episodeWord)}", videos)
		}
	}
	if (resolving != null && videos == null) {
		var seconds by remember(resolving.key) { mutableStateOf(0) }
		LaunchedEffect(resolving.key) { while (true) { kotlinx.coroutines.delay(1000); seconds++ } }
		KomiSheet(onDismiss = { vm.dismiss() }, title = resolving.label(episodeWord), titleJp = "読込中") {
			if (state?.videoError == null) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					KomiCircularProgress()
					KomiText(text = stringResource(R.string.loading_servers, seconds), role = KomiTextRole.Body, uppercase = false)
				}
			} else {
				KomiText(text = state?.videoError.orEmpty(), role = KomiTextRole.Body, color = colors.error, uppercase = false)
			}
			Spacer(Modifier.height(12.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				if (state?.videoError != null) {
					KomiButton(onClick = { vm.play(resolving) }, label = stringResource(R.string.retry), size = KomiButtonSize.Sm)
				}
				KomiButton(onClick = { vm.dismiss() }, label = stringResource(R.string.cancel), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
			}
			Spacer(Modifier.height(8.dp))
		}
	}
}
