package com.watanuki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import com.watanuki.app.R
import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.BrowseMode
import com.watanuki.app.ui.BrowseViewModel
import com.watanuki.app.download.DownloadItem
import com.watanuki.app.download.DownloadRepository
import com.watanuki.app.download.DownloadService
import com.watanuki.app.ui.DetailsViewModel
import com.watanuki.app.ui.SourcePrefs
import com.watanuki.app.ui.SourcesViewModel
import com.watanuki.app.ui.komi.KomiBadge
import com.watanuki.app.ui.komi.KomiBadgeTone
import com.watanuki.app.ui.komi.KomiBottomBar
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiChip
import com.watanuki.app.ui.komi.KomiCheckbox
import com.watanuki.app.ui.komi.KomiChipKind
import com.watanuki.app.ui.komi.KomiCircularProgress
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSegmented
import com.watanuki.app.ui.komi.KomiSegmentedItem
import com.watanuki.app.ui.komi.KomiSheet
import com.watanuki.app.ui.komi.KomiSurface
import com.watanuki.app.ui.komi.KomiSurfaceElevation
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextField
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.LocalPersonality
import com.watanuki.app.ui.komi.screentoneFill
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource

private fun LoadedSource.baseUrl(): String? = (source as? AnimeHttpSource)?.baseUrl

// ---------------------------------------------------------------- sources

@Composable
fun SourcesScreen(contentPadding: PaddingValues, onOpen: (LoadedSource) -> Unit) {
	val vm = viewModel<SourcesViewModel>()
	val sources by vm.sources.collectAsState()
	val failed by vm.failed.collectAsState()
	val colors = LocalPersonality.current.colors
	var enabled by remember { mutableStateOf<Set<Long>>(emptySet()) }
	LaunchedEffect(sources) { if (sources != null) enabled = SourcePrefs.enabledIds() }
	val list = sources?.filter { AppPrefs.showAdult || !it.isNsfw }
	if (list == null) {
		Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) { KomiCircularProgress() }
		return
	}
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 4.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item {
			KomiSectionHead(label = "${list.size} ${stringResource(R.string.sources)}", kicker = "ES")
			KomiText(text = stringResource(R.string.in_feed) + ": ${enabled.size}", role = KomiTextRole.Label, color = colors.onSurfaceVariant, uppercase = false)
			if (failed > 0) {
				KomiText(text = stringResource(R.string.sources_failed, failed), role = KomiTextRole.Label, color = colors.error, uppercase = false)
			}
		}
		item {
			KomiListContainer {
				list.forEachIndexed { index, src ->
					KomiListRow(
						title = src.name,
						subtitle = src.baseUrl(),
						onClick = { onOpen(src) },
						showDivider = index < list.lastIndex,
						trailing = {
							Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
								if (src.isNsfw) KomiBadge(text = "18+", tone = KomiBadgeTone.Alert, tilt = true)
								KomiCheckbox(
									checked = src.id in enabled,
									onCheckedChange = { on ->
										SourcePrefs.setEnabled(src.id, on)
										enabled = SourcePrefs.enabledIds()
									},
								)
							}
						},
					)
				}
			}
		}
	}
}

// ---------------------------------------------------------------- browse

@Composable
fun BrowseScreen(source: LoadedSource, onOpen: (SAnime) -> Unit, onBack: () -> Unit, initialQuery: String = "") {
	val vm = viewModel<BrowseViewModel>()
	val state by vm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	var query by rememberSaveable { mutableStateOf(initialQuery) }
	LaunchedEffect(source.id, initialQuery) {
		if (initialQuery.isNotBlank()) vm.open(source, BrowseMode.SEARCH, initialQuery) else vm.open(source)
	}
	val gridState = rememberLazyGridState()
	val nearEnd by remember {
		derivedStateOf {
			val info = gridState.layoutInfo
			val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
			info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
		}
	}
	LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = source.name,
				subtitle = source.baseUrl(),
				leading = { KomiButton(onClick = onBack, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
			)
		},
	) { padding ->
		Column(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
			Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
				KomiSegmented(
					selected = state.mode,
					items = listOf(
						KomiSegmentedItem(BrowseMode.POPULAR, stringResource(R.string.popular)),
						KomiSegmentedItem(BrowseMode.LATEST, stringResource(R.string.latest)),
					) + if (state.mode == BrowseMode.SEARCH) listOf(KomiSegmentedItem(BrowseMode.SEARCH, stringResource(R.string.search))) else emptyList(),
					onSelect = { if (it != BrowseMode.SEARCH) vm.open(source, it) },
					height = 36.dp,
				)
			}
			Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
				KomiTextField(
					value = query,
					onValueChange = { query = it },
					modifier = Modifier.weight(1f),
					placeholder = stringResource(R.string.search),
					onCommit = { if (query.isNotBlank()) vm.open(source, BrowseMode.SEARCH, query.trim()) },
				)
				KomiButton(onClick = { if (query.isNotBlank()) vm.open(source, BrowseMode.SEARCH, query.trim()) }, label = stringResource(R.string.search), size = KomiButtonSize.Sm, enabled = query.isNotBlank())
			}
			LazyVerticalGrid(
				columns = GridCells.Adaptive(112.dp),
				state = gridState,
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = padding.calculateBottomPadding() + 24.dp),
				horizontalArrangement = Arrangement.spacedBy(10.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				items(state.items, key = { it.url }) { anime ->
					Column(Modifier.clickable { onOpen(anime) }) {
						Cover(url = anime.thumbnail_url, referer = source.baseUrl(), modifier = Modifier.fillMaxWidth())
						KomiText(text = anime.title, role = KomiTextRole.Label, uppercase = false, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
					}
				}
				if (state.isLoading) {
					item(span = { GridItemSpan(maxLineSpan) }) {
						Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { KomiCircularProgress() }
					}
				}
				state.error?.let { err ->
					item(span = { GridItemSpan(maxLineSpan) }) {
						Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
							KomiText(text = err, role = KomiTextRole.Body, color = colors.error, uppercase = false, textAlign = TextAlign.Center)
							KomiButton(onClick = { vm.loadMore() }, label = stringResource(R.string.retry), variant = KomiButtonVariant.Outline)
						}
					}
				}
				if (!state.isLoading && state.error == null && state.items.isEmpty()) {
					item(span = { GridItemSpan(maxLineSpan) }) {
						KomiText(text = stringResource(R.string.nothing_found), role = KomiTextRole.Title, color = colors.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
					}
				}
			}
		}
	}
}

// ---------------------------------------------------------------- details

@Composable
fun DetailsScreen(source: LoadedSource, anime: SAnime, onPlay: (String, Video) -> Unit, onBack: () -> Unit) {
	val vm = viewModel<DetailsViewModel>()
	val state by vm.state.collectAsState()
	val colors = LocalPersonality.current.colors
	LaunchedEffect(source.id, anime.url) { vm.open(source, anime) }
	val details = state.anime ?: anime
	val downloads by DownloadRepository.items.collectAsState()

	KomiScaffold(
		topBar = {
			KomiTopBar(
				title = details.title,
				subtitle = source.name,
				leading = { KomiButton(onClick = onBack, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
			)
		},
	) { padding ->
		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
		) {
			item {
				KomiSurface(modifier = Modifier.fillMaxWidth(), elevation = KomiSurfaceElevation.Card, contentPadding = PaddingValues(13.dp)) {
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						Cover(url = details.thumbnail_url, referer = source.baseUrl(), modifier = Modifier.width(110.dp))
						Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
							KomiText(text = details.title, role = KomiTextRole.Title, maxLines = 3, overflow = TextOverflow.Ellipsis)
							val status = when (details.status) {
								SAnime.ONGOING -> "En emisión"
								SAnime.COMPLETED -> "Finalizado"
								else -> null
							}
							Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
								status?.let { KomiChip(label = it, kind = KomiChipKind.Filter, selected = true, small = true, tilt = false) }
								details.author?.takeIf { it.isNotBlank() }?.let { KomiChip(label = it, kind = KomiChipKind.Info, small = true, tilt = false) }
							}
							details.getGenres()?.takeIf { it.isNotEmpty() }?.let { genres ->
								KomiText(text = genres.joinToString(" · "), role = KomiTextRole.Label, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, maxLines = 3, overflow = TextOverflow.Ellipsis)
							}
						}
					}
				}
			}
			details.description?.takeIf { it.isNotBlank() }?.let { desc ->
				item { KomiText(text = desc, role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false) }
			}
			item {
				val context = LocalContext.current
				KomiSectionHead(
					label = "${state.episodes.size} ${stringResource(R.string.episodes)}", kicker = "話",
					action = if (state.episodes.isNotEmpty()) {
						{
							KomiButton(
								onClick = {
									val queued = state.episodes.count { ep -> DownloadRepository.enqueueEpisode(source, details, ep) != null }
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
				state.isLoading -> item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { KomiCircularProgress() } }
				state.error != null -> item {
					Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
						KomiText(text = state.error.orEmpty(), role = KomiTextRole.Body, color = colors.error, uppercase = false, textAlign = TextAlign.Center)
						KomiButton(onClick = { vm.open(source, anime) }, label = stringResource(R.string.retry), variant = KomiButtonVariant.Outline)
					}
				}
				else -> item {
					KomiListContainer {
						state.episodes.forEachIndexed { index, ep ->
							KomiListRow(
								title = ep.name,
								subtitle = ep.scanlator,
								onClick = { vm.loadVideos(ep, autoPlay = AppPrefs.autoSelectServer) },
								onLongClick = { vm.loadVideos(ep, autoPlay = false) },
								showDivider = index < state.episodes.lastIndex,
								trailing = {
									val context = LocalContext.current
									val existing = downloads.firstOrNull { it.sourceId == source.id && it.episodeUrl == ep.url }
									Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
										when (existing?.status) {
											DownloadItem.STATUS_DONE -> KomiBadge(text = "✓", tone = KomiBadgeTone.Neutral)
											DownloadItem.STATUS_RUNNING, DownloadItem.STATUS_QUEUED -> KomiBadge(text = "…", tone = KomiBadgeTone.Neutral)
											else -> KomiButton(
												onClick = {
													if (DownloadRepository.enqueueEpisode(source, details, ep) != null) {
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
						}
					}
				}
			}
		}
	}

	// auto mode: play as soon as the first server is known
	state.autoPlayVideo?.let { video ->
		LaunchedEffect(video) {
			val ep = state.videosFor
			vm.consumeAutoPlay()
			if (ep != null) onPlay("${details.title} · ${ep.name}", video)
		}
	}

	val forEpisode = state.videosFor
	if (forEpisode != null && state.autoPlay && state.autoPlayVideo == null) {
		var seconds by remember(forEpisode) { mutableStateOf(0) }
		LaunchedEffect(forEpisode) { while (true) { kotlinx.coroutines.delay(1000); seconds++ } }
		KomiSheet(onDismiss = { vm.dismissVideos() }, title = forEpisode.name, titleJp = "読込中") {
			if (state.isLoadingVideos) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					KomiCircularProgress()
					KomiText(text = stringResource(R.string.loading_servers, seconds), role = KomiTextRole.Body, uppercase = false)
				}
			} else {
				val message = when (state.videoError) {
					"timeout" -> stringResource(R.string.server_timeout)
					null -> stringResource(R.string.no_videos)
					else -> state.videoError.orEmpty()
				}
				KomiText(text = message, role = KomiTextRole.Body, color = colors.error, uppercase = false)
			}
			Spacer(Modifier.height(12.dp))
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				if (!state.isLoadingVideos) {
					KomiButton(onClick = { vm.loadVideos(forEpisode, autoPlay = true) }, label = stringResource(R.string.retry), size = KomiButtonSize.Sm)
				}
				KomiButton(onClick = { vm.chooseManually() }, label = stringResource(R.string.choose_manually), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Tonal)
				KomiButton(onClick = { vm.dismissVideos() }, label = stringResource(R.string.cancel), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
			}
			Spacer(Modifier.height(8.dp))
		}
	} else if (forEpisode != null && !state.autoPlay) {
		val context = LocalContext.current
		KomiSheet(onDismiss = { vm.dismissVideos() }, title = stringResource(R.string.choose_video), titleJp = forEpisode.name) {
			when {
				state.isLoadingVideos -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { KomiCircularProgress() }
				state.videoError != null -> KomiText(text = state.videoError.orEmpty(), role = KomiTextRole.Body, color = colors.error, uppercase = false)
				state.videos.isNullOrEmpty() -> KomiText(text = stringResource(R.string.no_videos), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false)
				else -> KomiListContainer {
					val videos = state.videos.orEmpty()
					videos.forEachIndexed { index, video ->
						KomiListRow(
							title = video.quality,
							subtitle = (video.videoUrl ?: video.url).substringAfter("://").substringBefore("/"),
							onClick = { vm.dismissVideos(); onPlay("${details.title} · ${forEpisode.name}", video) },
							showDivider = index < videos.lastIndex,
							trailing = {
								KomiButton(
									onClick = {
										val headers = buildMap {
											video.headers?.forEach { (k, v) -> put(k, v) }
											source.baseUrl()?.let { if (!containsKey("Referer")) put("Referer", it) }
										}
										val queued = DownloadRepository.enqueue(source, details, forEpisode, video.videoUrl ?: video.url, headers)
										if (queued != null) {
											DownloadService.start(context)
											android.widget.Toast.makeText(context, R.string.download_started, android.widget.Toast.LENGTH_SHORT).show()
										} else {
											android.widget.Toast.makeText(context, R.string.already_downloaded, android.widget.Toast.LENGTH_SHORT).show()
										}
										vm.dismissVideos()
									},
									label = stringResource(R.string.download),
									size = KomiButtonSize.Sm,
									variant = KomiButtonVariant.Outline,
								)
							},
						)
					}
				}
			}
			Spacer(Modifier.height(8.dp))
		}
	}
}

// ---------------------------------------------------------------- shared

@Composable
fun Cover(url: String?, referer: String?, modifier: Modifier = Modifier) {
	val colors = LocalPersonality.current.colors
	val context = LocalContext.current
	Box(
		modifier = modifier
			.aspectRatio(2f / 3f)
			.background(colors.surfaceVariant)
			.screentoneFill(color = colors.onSurface, opacity = colors.screentoneOpacity + 0.04f)
			.border(2.5.dp, colors.outline)
			.padding(2.5.dp)
			.clipToBounds(),
		contentAlignment = Alignment.Center,
	) {
		if (!url.isNullOrBlank()) {
			val request = ImageRequest.Builder(context).data(url).apply {
				if (referer != null) httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
			}.build()
			AsyncImage(model = request, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
		}
	}
}
