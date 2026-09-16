package com.watanuki.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.watanuki.app.download.DownloadItem
import com.watanuki.app.download.DownloadRepository
import com.watanuki.app.player.PlayerActivity
import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.komi.KomiBottomBar
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiIconButton
import com.watanuki.app.ui.komi.KomiNavItem
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.WatanukiTheme
import com.watanuki.app.ui.screens.BrowseScreen
import com.watanuki.app.ui.screens.DetailsScreen
import com.watanuki.app.ui.screens.DownloadsScreen
import com.watanuki.app.catalog.CatalogAnime
import com.watanuki.app.catalog.CatalogSeries
import com.watanuki.app.catalog.SourceMatch
import com.watanuki.app.ui.screens.GlobalSearchScreen
import com.watanuki.app.ui.screens.SeasonScreen
import com.watanuki.app.ui.screens.SeriesScreen
import com.watanuki.app.ui.screens.HomeScreen
import com.watanuki.app.ui.screens.OnboardingScreen
import com.watanuki.app.ui.screens.SettingsScreen
import com.watanuki.app.ui.screens.SourcesScreen
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource

class MainActivity : ComponentActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		requestNotificationPermission()
		setContent {
			WatanukiTheme {
				if (AppPrefs.onboardingDone) {
					WatanukiNav(onPlay = ::play, onPlayLocal = ::playLocal)
				} else {
					OnboardingScreen(onDone = {})
				}
			}
		}
	}

	private fun play(source: LoadedSource?, title: String, videos: List<Video>) {
		startActivity(PlayerActivity.intent(this, title, videos, (source?.source as? AnimeHttpSource)?.baseUrl))
	}

	private fun playLocal(item: DownloadItem) {
		startActivity(PlayerActivity.localIntent(this, "${item.animeTitle} · ${item.episodeName}", DownloadRepository.file(item).absolutePath))
	}

	private fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
			ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
		) {
			ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
		}
	}
}

private enum class Tab(val id: String) { HOME("home"), DOWNLOADS("downloads"), SETTINGS("settings") }

private sealed interface Screen {
	data class Search(val query: String = "", val bySource: Boolean = false) : Screen
	data object Sources : Screen
	data class Series(val series: CatalogSeries) : Screen
	data class Season(val season: CatalogAnime, val hits: List<SourceMatch.Hit>) : Screen
	data class Browse(val source: LoadedSource, val query: String = "") : Screen
	data class Details(val source: LoadedSource, val anime: SAnime) : Screen
}

@Composable
private fun WatanukiNav(onPlay: (LoadedSource?, String, List<Video>) -> Unit, onPlayLocal: (DownloadItem) -> Unit) {
	var tab by remember { mutableStateOf(Tab.HOME) }
	var stack by remember { mutableStateOf<List<Screen>>(emptyList()) }
	val current = stack.lastOrNull()
	BackHandler(enabled = stack.isNotEmpty()) { stack = stack.dropLast(1) }

	when (current) {
		is Screen.Search -> GlobalSearchScreen(
			initialQuery = current.query,
			bySource = current.bySource,
			onOpen = { feed -> stack = stack + Screen.Details(feed.source, feed.anime) },
			onOpenSource = { src, query -> stack = stack + Screen.Browse(src, query) },
			onOpenSeries = { series -> stack = stack + Screen.Series(series) },
			onBack = { stack = stack.dropLast(1) },
		)
		is Screen.Season -> SeasonScreen(
			season = current.season,
			hits = current.hits,
			onPlay = { title, videos -> onPlay(null, title, videos) },
			onBack = { stack = stack.dropLast(1) },
		)
		is Screen.Sources -> KomiScaffold(
			topBar = {
				KomiTopBar(
					title = stringResource(R.string.sources), subtitle = "配信 · ${stringResource(R.string.sources_kicker)}",
					leading = { KomiButton(onClick = { stack = stack.dropLast(1) }, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline) },
				)
			},
		) { padding -> SourcesScreen(contentPadding = padding, onOpen = { stack = stack + Screen.Browse(it) }) }
		is Screen.Series -> SeriesScreen(
			series = current.series,
			onOpenSeason = { season, hits -> stack = stack + Screen.Season(season, hits) },
			onSearchBySource = { title -> stack = stack + Screen.Search(query = title, bySource = true) },
			onBack = { stack = stack.dropLast(1) },
		)
		is Screen.Browse -> BrowseScreen(
			source = current.source,
			initialQuery = current.query,
			onOpen = { anime -> stack = stack + Screen.Details(current.source, anime) },
			onBack = { stack = stack.dropLast(1) },
		)
		is Screen.Details -> DetailsScreen(
			source = current.source,
			anime = current.anime,
			onPlay = { title, videos -> onPlay(current.source, title, videos) },
			onBack = { stack = stack.dropLast(1) },
		)
		null -> {
			val items = listOf(
				KomiNavItem(Tab.HOME.id, stringResource(R.string.home), ImageVector.vectorResource(R.drawable.ic_home)),
				KomiNavItem(Tab.DOWNLOADS.id, stringResource(R.string.downloads), ImageVector.vectorResource(R.drawable.ic_downloads)),
				KomiNavItem(Tab.SETTINGS.id, stringResource(R.string.settings), ImageVector.vectorResource(R.drawable.ic_settings)),
			)
			val kicker = when (tab) {
				Tab.HOME -> "今日 · ${stringResource(R.string.for_you).uppercase()}"
				Tab.DOWNLOADS -> "保存 · ${stringResource(R.string.downloads).uppercase()}"
				Tab.SETTINGS -> "設定 · ${stringResource(R.string.settings).uppercase()}"
			}
			KomiScaffold(
				topBar = {
					KomiTopBar(
						title = stringResource(R.string.app_name), titleAccent = "nuki", subtitle = kicker,
						actions = if (tab == Tab.HOME) {
							{
								KomiIconButton(
									icon = ImageVector.vectorResource(R.drawable.ic_search),
									contentDescription = stringResource(R.string.global_search),
									onClick = { stack = stack + Screen.Search() },
								)
							}
						} else null,
					)
				},
				bottomBar = { KomiBottomBar(items = items, selectedId = tab.id, onSelect = { id -> tab = Tab.entries.first { it.id == id } }) },
			) { padding ->
				when (tab) {
					Tab.HOME -> HomeScreen(
						contentPadding = padding,
						onOpen = { feed -> stack = stack + Screen.Details(feed.source, feed.anime) },
						onOpenSource = { src -> stack = stack + Screen.Browse(src) },
					)
					Tab.DOWNLOADS -> DownloadsScreen(contentPadding = padding, onPlay = onPlayLocal)
					Tab.SETTINGS -> SettingsScreen(contentPadding = padding, onOpenDownloads = { tab = Tab.DOWNLOADS }, onOpenSources = { stack = stack + Screen.Sources })
				}
			}
		}
	}
}
