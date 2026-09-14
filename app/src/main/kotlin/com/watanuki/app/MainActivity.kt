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
import com.watanuki.app.ui.komi.KomiIconButton
import com.watanuki.app.ui.komi.KomiNavItem
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.WatanukiTheme
import com.watanuki.app.ui.screens.BrowseScreen
import com.watanuki.app.ui.screens.DetailsScreen
import com.watanuki.app.ui.screens.DownloadsScreen
import com.watanuki.app.ui.screens.GlobalSearchScreen
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

	private fun play(source: LoadedSource, title: String, video: Video) {
		startActivity(PlayerActivity.intent(this, title, video, (source.source as? AnimeHttpSource)?.baseUrl))
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

private enum class Tab(val id: String) { HOME("home"), SOURCES("sources"), DOWNLOADS("downloads"), SETTINGS("settings") }

private sealed interface Screen {
	data object Search : Screen
	data class Browse(val source: LoadedSource, val query: String = "") : Screen
	data class Details(val source: LoadedSource, val anime: SAnime) : Screen
}

@Composable
private fun WatanukiNav(onPlay: (LoadedSource, String, Video) -> Unit, onPlayLocal: (DownloadItem) -> Unit) {
	var tab by remember { mutableStateOf(Tab.HOME) }
	var stack by remember { mutableStateOf<List<Screen>>(emptyList()) }
	val current = stack.lastOrNull()
	BackHandler(enabled = stack.isNotEmpty()) { stack = stack.dropLast(1) }

	when (current) {
		is Screen.Search -> GlobalSearchScreen(
			onOpen = { feed -> stack = stack + Screen.Details(feed.source, feed.anime) },
			onOpenSource = { src, query -> stack = stack + Screen.Browse(src, query) },
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
			onPlay = { title, video -> onPlay(current.source, title, video) },
			onBack = { stack = stack.dropLast(1) },
		)
		null -> {
			val items = listOf(
				KomiNavItem(Tab.HOME.id, stringResource(R.string.home), ImageVector.vectorResource(R.drawable.ic_home)),
				KomiNavItem(Tab.SOURCES.id, stringResource(R.string.sources), ImageVector.vectorResource(R.drawable.ic_sources)),
				KomiNavItem(Tab.DOWNLOADS.id, stringResource(R.string.downloads), ImageVector.vectorResource(R.drawable.ic_downloads)),
				KomiNavItem(Tab.SETTINGS.id, stringResource(R.string.settings), ImageVector.vectorResource(R.drawable.ic_settings)),
			)
			val kicker = when (tab) {
				Tab.HOME -> "今日 · ${stringResource(R.string.for_you).uppercase()}"
				Tab.SOURCES -> "配信 · ${stringResource(R.string.sources).uppercase()}"
				Tab.DOWNLOADS -> "保存 · ${stringResource(R.string.downloads).uppercase()}"
				Tab.SETTINGS -> "設定 · ${stringResource(R.string.settings).uppercase()}"
			}
			KomiScaffold(
				topBar = {
					KomiTopBar(
						title = stringResource(R.string.app_name), titleAccent = "nuki", subtitle = kicker,
						actions = if (tab == Tab.HOME || tab == Tab.SOURCES) {
							{
								KomiIconButton(
									icon = ImageVector.vectorResource(R.drawable.ic_search),
									contentDescription = stringResource(R.string.global_search),
									onClick = { stack = stack + Screen.Search },
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
					Tab.SOURCES -> SourcesScreen(contentPadding = padding, onOpen = { stack = stack + Screen.Browse(it) })
					Tab.DOWNLOADS -> DownloadsScreen(contentPadding = padding, onPlay = onPlayLocal)
					Tab.SETTINGS -> SettingsScreen(contentPadding = padding, onOpenDownloads = { tab = Tab.DOWNLOADS })
				}
			}
		}
	}
}
