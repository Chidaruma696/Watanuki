package com.watanuki.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.watanuki.app.BuildConfig
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiCircularProgress
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.WatanukiTheme
import com.watanuki.sources.SourcesRuntime
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.delay
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout
import java.util.Locale

/**
 * libVLC-backed player. Streams go through [StreamProxy] so every header and cookie the
 * source needs reaches the video host. Milestone 1 controls: play, pause, seek, scale modes.
 */
class PlayerActivity : ComponentActivity() {

	private lateinit var libVlc: LibVLC
	private lateinit var player: MediaPlayer
	private var proxy: StreamProxy? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		WindowCompat.setDecorFitsSystemWindows(window, false)
		WindowInsetsControllerCompat(window, window.decorView).apply {
			hide(WindowInsetsCompat.Type.systemBars())
			systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}

		val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
		val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
		val headers = intent.getStringArrayExtra(EXTRA_HEADERS).orEmpty().toList().chunked(2).associate { it[0] to it[1] }
			.toMutableMap()
		if (headers.keys.none { it.equals("User-Agent", true) }) {
			headers["User-Agent"] = SourcesRuntime.network.defaultUserAgentProvider()
		}

		val isLocal = url.startsWith("/") || url.startsWith("file:")
		val playUrl = if (isLocal) {
			url
		} else try {
			proxy = StreamProxy().also { it.start(NANO_TIMEOUT, false) }
			proxy!!.proxiedUrl(url, headers)
		} catch (e: Exception) {
			Log.w(TAG, "proxy unavailable, playing direct", e)
			url
		}
		Log.i(TAG, "play $url via $playUrl headers=${headers.keys}")

		val options = arrayListOf("--http-reconnect", "--network-caching=4000")
		if (BuildConfig.DEBUG) options += "-vv"
		libVlc = LibVLC(this, options)
		player = MediaPlayer(libVlc)
		val media = (if (isLocal) Media(libVlc, playUrl.removePrefix("file://")) else Media(libVlc, Uri.parse(playUrl))).apply {
			setHWDecoderEnabled(true, false)
			headers["User-Agent"]?.let { addOption(":http-user-agent=$it") }
			headers["Referer"]?.let { addOption(":http-referrer=$it") }
		}
		player.media = media
		media.release()

		setContent {
			WatanukiTheme {
				PlayerScreen(player = player, title = title, onClose = { finish() })
			}
		}
	}

	override fun onPause() {
		super.onPause()
		if (::player.isInitialized && player.isPlaying) player.pause()
	}

	override fun onDestroy() {
		super.onDestroy()
		if (::player.isInitialized) {
			player.stop()
			player.detachViews()
			player.release()
			libVlc.release()
		}
		proxy?.stop()
	}

	companion object {
		private const val TAG = "Player"
		private const val NANO_TIMEOUT = 30_000
		private const val EXTRA_URL = "url"
		private const val EXTRA_TITLE = "title"
		private const val EXTRA_HEADERS = "headers"

		fun localIntent(context: Context, title: String, path: String): Intent =
			Intent(context, PlayerActivity::class.java).putExtra(EXTRA_URL, path).putExtra(EXTRA_TITLE, title)

		fun intent(context: Context, title: String, video: Video, referer: String?): Intent {
			val headers = ArrayList<String>()
			video.headers?.forEach { (k, v) -> headers += k; headers += v }
			if (referer != null && video.headers?.get("Referer") == null) {
				headers += "Referer"; headers += referer
			}
			return Intent(context, PlayerActivity::class.java)
				.putExtra(EXTRA_URL, video.videoUrl ?: video.url)
				.putExtra(EXTRA_TITLE, title)
				.putExtra(EXTRA_HEADERS, headers.toTypedArray())
		}
	}
}

private val ScaleModes = listOf(
	MediaPlayer.ScaleType.SURFACE_BEST_FIT to "Ajustar",
	MediaPlayer.ScaleType.SURFACE_FIT_SCREEN to "Rellenar",
	MediaPlayer.ScaleType.SURFACE_FILL to "Estirar",
	MediaPlayer.ScaleType.SURFACE_16_9 to "16:9",
	MediaPlayer.ScaleType.SURFACE_4_3 to "4:3",
	MediaPlayer.ScaleType.SURFACE_ORIGINAL to "Original",
)

@Composable
private fun PlayerScreen(player: MediaPlayer, title: String, onClose: () -> Unit) {
	var controlsVisible by remember { mutableStateOf(true) }
	var isPlaying by remember { mutableStateOf(false) }
	var position by remember { mutableFloatStateOf(0f) }
	var length by remember { mutableLongStateOf(0L) }
	var timeMs by remember { mutableLongStateOf(0L) }
	var scaleIndex by remember { mutableIntStateOf(1) }
	var buffering by remember { mutableStateOf(true) }
	var error by remember { mutableStateOf<String?>(null) }

	DisposableEffect(player) {
		val listener = MediaPlayer.EventListener { event ->
			when (event.type) {
				MediaPlayer.Event.Opening -> buffering = true
				MediaPlayer.Event.Playing -> { isPlaying = true; buffering = false; error = null }
				MediaPlayer.Event.Paused, MediaPlayer.Event.Stopped -> isPlaying = false
				MediaPlayer.Event.Buffering -> buffering = event.buffering < 100f
				MediaPlayer.Event.PositionChanged -> position = event.positionChanged
				MediaPlayer.Event.TimeChanged -> timeMs = event.timeChanged
				MediaPlayer.Event.LengthChanged -> length = event.lengthChanged
				MediaPlayer.Event.EncounteredError -> { buffering = false; error = "VLC no pudo abrir el vídeo (servidor caído, enlace caducado o formato no soportado)" }
				MediaPlayer.Event.EndReached -> onClose()
			}
		}
		player.setEventListener(listener)
		onDispose { player.setEventListener(null) }
	}
	LaunchedEffect(controlsVisible, isPlaying) {
		if (controlsVisible && isPlaying) {
			delay(4000)
			controlsVisible = false
		}
	}

	Box(Modifier.fillMaxSize().background(Color.Black)) {
		AndroidView(
			factory = { context ->
				VLCVideoLayout(context).also { layout ->
					player.attachViews(layout, null, false, false)
					player.videoScale = ScaleModes[scaleIndex].first
					player.play()
				}
			},
			modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { controlsVisible = !controlsVisible },
		)
		if (buffering && error == null) {
			Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { KomiCircularProgress(color = Color.White) }
		}
		error?.let { msg ->
			Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
				Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
					KomiText(text = msg, role = KomiTextRole.Body, color = Color.White, uppercase = false, textAlign = TextAlign.Center)
					KomiButton(onClick = { error = null; player.stop(); player.play() }, label = "Reintentar", size = KomiButtonSize.Sm)
				}
			}
		}
		if (controlsVisible) {
			Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
				Row(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
					KomiButton(onClick = onClose, label = "‹", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
					KomiText(text = title, role = KomiTextRole.Label, color = Color.White, uppercase = false, maxLines = 1, modifier = Modifier.weight(1f))
					KomiButton(
						onClick = {
							scaleIndex = (scaleIndex + 1) % ScaleModes.size
							player.videoScale = ScaleModes[scaleIndex].first
						},
						label = ScaleModes[scaleIndex].second,
						size = KomiButtonSize.Sm,
						variant = KomiButtonVariant.Tonal,
					)
				}
				Spacer(Modifier.weight(1f))
				Column(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 10.dp)) {
					Slider(
						value = position,
						onValueChange = { position = it; player.position = it },
						colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
					)
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
						KomiButton(
							onClick = { if (player.isPlaying) player.pause() else player.play() },
							label = if (isPlaying) "❚❚" else "▶",
							size = KomiButtonSize.Sm,
						)
						KomiButton(onClick = { player.time = (player.time - 10_000).coerceAtLeast(0) }, label = "-10s", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
						KomiButton(onClick = { player.time = player.time + 30_000 }, label = "+30s", size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
						Spacer(Modifier.width(4.dp))
						KomiText(text = "${fmt(timeMs)} / ${fmt(length)}", role = KomiTextRole.Mono, color = Color.White)
					}
				}
			}
		}
	}
}

private fun fmt(ms: Long): String {
	val s = ms / 1000
	val h = s / 3600
	val m = (s % 3600) / 60
	val sec = s % 60
	return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
}
