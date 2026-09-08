package com.watanuki.app.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.StatFs
import android.util.Log
import androidx.core.app.NotificationCompat
import com.watanuki.app.MainActivity
import com.watanuki.app.R
import com.watanuki.app.ui.AppPrefs
import com.watanuki.sources.AnimeSources
import com.watanuki.sources.LoadedSource
import com.watanuki.sources.SourcesRuntime
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** One saved (or in-progress) episode. [url] is empty until the service resolves the video. */
@Serializable
data class DownloadItem(
	val id: String,
	val animeTitle: String,
	val episodeName: String,
	val sourceName: String,
	val url: String,
	val headers: Map<String, String>,
	val fileName: String,
	val thumbnail: String? = null,
	val status: String = STATUS_QUEUED,
	val bytes: Long = 0,
	val total: Long = -1,
	val error: String? = null,
	val sourceId: Long? = null,
	val episodeUrl: String? = null,
	val episodeNumber: Float = -1f,
	val addedAt: Long = System.currentTimeMillis(),
) {
	/** Same source + episode means the same download, whatever the server picked. */
	val key: String get() = "$sourceId|${episodeUrl ?: url}"

	companion object {
		const val STATUS_QUEUED = "queued"
		const val STATUS_RUNNING = "running"
		const val STATUS_DONE = "done"
		const val STATUS_FAILED = "failed"
	}
}

/**
 * Downloads live in the app's external files dir (no storage permission needed) with a
 * JSON index next to them. Items can be enqueued before their video link is known: the
 * service resolves the servers through the source when the download starts.
 */
object DownloadRepository {

	val items = MutableStateFlow<List<DownloadItem>>(emptyList())
	private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
	private lateinit var dir: File
	private lateinit var index: File
	private val lock = Any()

	fun init(context: Context) {
		dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }
		index = File(dir, "index.json")
		items.value = runCatching { json.decodeFromString<List<DownloadItem>>(index.readText()) }.getOrDefault(emptyList())
			// anything that was mid-flight when the app died is queued again
			.map { if (it.status == DownloadItem.STATUS_RUNNING) it.copy(status = DownloadItem.STATUS_QUEUED) else it }
			// a "done" record whose file vanished is not done
			.map { if (it.status == DownloadItem.STATUS_DONE && !File(dir, it.fileName).exists()) it.copy(status = DownloadItem.STATUS_FAILED, error = "Archivo eliminado") else it }
	}

	fun file(item: DownloadItem): File = File(dir, item.fileName)

	fun freeBytes(): Long = runCatching { StatFs(dir.absolutePath).availableBytes }.getOrDefault(Long.MAX_VALUE)

	fun find(source: LoadedSource, episode: SEpisode): DownloadItem? =
		items.value.firstOrNull { it.sourceId == source.id && it.episodeUrl == episode.url }

	/** Queues an episode; the video link is resolved by the service. Returns null if already queued or saved. */
	fun enqueueEpisode(source: LoadedSource, anime: SAnime, episode: SEpisode): DownloadItem? = synchronized(lock) {
		val existing = find(source, episode)
		if (existing != null && existing.status != DownloadItem.STATUS_FAILED) return null
		if (existing != null) removeInternal(existing)
		val safe = "${anime.title.take(60)} - ${episode.name.take(40)}".replace(Regex("""[\\/:*?"<>|]"""), "_")
		val item = DownloadItem(
			id = UUID.randomUUID().toString(),
			animeTitle = anime.title,
			episodeName = episode.name,
			sourceName = source.name,
			url = "",
			headers = emptyMap(),
			fileName = "$safe.mp4",
			thumbnail = anime.thumbnail_url,
			sourceId = source.id,
			episodeUrl = episode.url,
			episodeNumber = episode.episode_number,
		)
		items.update { it + item }
		persist()
		item
	}

	/** Queues a concrete video link (server chosen by the user). */
	fun enqueue(source: LoadedSource, anime: SAnime, episode: SEpisode, url: String, headers: Map<String, String>): DownloadItem? = synchronized(lock) {
		val existing = find(source, episode)
		if (existing != null && existing.status == DownloadItem.STATUS_DONE) return null
		if (existing != null) removeInternal(existing)
		val isHls = url.substringBefore('?').endsWith(".m3u8", true)
		val safe = "${anime.title.take(60)} - ${episode.name.take(40)}".replace(Regex("""[\\/:*?"<>|]"""), "_")
		val item = DownloadItem(
			id = UUID.randomUUID().toString(),
			animeTitle = anime.title,
			episodeName = episode.name,
			sourceName = source.name,
			url = url,
			headers = headers,
			fileName = safe + if (isHls) ".ts" else ".mp4",
			thumbnail = anime.thumbnail_url,
			sourceId = source.id,
			episodeUrl = episode.url,
			episodeNumber = episode.episode_number,
		)
		items.update { it + item }
		persist()
		item
	}

	fun update(id: String, transform: (DownloadItem) -> DownloadItem) = synchronized(lock) {
		items.update { list -> list.map { if (it.id == id) transform(it) else it } }
		persist()
	}

	fun remove(item: DownloadItem) = synchronized(lock) {
		removeInternal(item)
		persist()
	}

	private fun removeInternal(item: DownloadItem) {
		file(item).delete()
		File(dir, item.fileName.substringBeforeLast('.') + ".ts").delete()
		items.update { list -> list.filterNot { it.id == item.id } }
	}

	/** Atomically claims the next queued item for a worker. */
	fun claimNext(): DownloadItem? = synchronized(lock) {
		val next = items.value.firstOrNull { it.status == DownloadItem.STATUS_QUEUED } ?: return null
		val claimed = next.copy(status = DownloadItem.STATUS_RUNNING, error = null)
		items.update { list -> list.map { if (it.id == next.id) claimed else it } }
		persist()
		claimed
	}

	fun hasQueued(): Boolean = items.value.any { it.status == DownloadItem.STATUS_QUEUED }

	private fun persist() {
		runCatching { index.writeText(json.encodeToString(items.value)) }
			.onFailure { Log.w("Downloads", "index write failed", it) }
	}
}

/** Foreground service: N parallel workers drain the queue, one notification summarises them. */
class DownloadService : Service() {

	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val workers = ArrayList<Job>()
	private val active = AtomicInteger(0)

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForegroundCompat(buildNotification(getString(R.string.downloads), getString(R.string.download_preparing), 0, true))
		val wanted = AppPrefs.parallelDownloads.coerceIn(1, 4)
		workers.removeAll { !it.isActive }
		while (workers.size < wanted) {
			workers += scope.launch { worker() }
		}
		return START_STICKY
	}

	private suspend fun worker() {
		active.incrementAndGet()
		try {
			while (true) {
				val item = DownloadRepository.claimNext() ?: break
				try {
					val resolved = resolveIfNeeded(item)
					checkSpace(resolved)
					download(resolved)
					DownloadRepository.update(item.id) { it.copy(status = DownloadItem.STATUS_DONE) }
				} catch (e: Throwable) {
					if (e is kotlinx.coroutines.CancellationException) throw e
					Log.w("Downloads", "download failed: ${item.animeTitle} ${item.episodeName}", e)
					DownloadRepository.update(item.id) { it.copy(status = DownloadItem.STATUS_FAILED, error = e.message ?: e.javaClass.simpleName) }
				}
			}
		} finally {
			if (active.decrementAndGet() == 0 && !DownloadRepository.hasQueued()) {
				stopForeground(STOP_FOREGROUND_REMOVE)
				stopSelf()
			}
		}
	}

	/** Items queued from "download season" carry no link yet: ask the source for the servers now. */
	private suspend fun resolveIfNeeded(item: DownloadItem): DownloadItem {
		if (item.url.isNotBlank()) return item
		val source = item.sourceId?.let { AnimeSources.byId(it) } ?: error("Fuente no disponible")
		val episode = SEpisode.create().apply { url = item.episodeUrl.orEmpty(); name = item.episodeName; episode_number = item.episodeNumber }
		notify(buildNotification("${item.animeTitle} · ${item.episodeName}", getString(R.string.download_resolving), 0, true))
		val videos = withTimeout(RESOLVE_TIMEOUT) { source.source.getVideoList(episode) }
		val video = videos.firstOrNull { !it.videoUrl.isNullOrBlank() || it.url.isNotBlank() } ?: error("Sin servidores disponibles")
		val url = video.videoUrl ?: video.url
		val headers = buildMap {
			video.headers?.forEach { (k, v) -> put(k, v) }
			(source.source as? AnimeHttpSource)?.baseUrl?.let { if (!containsKey("Referer")) put("Referer", it) }
		}
		val isHls = url.substringBefore('?').endsWith(".m3u8", true)
		val resolved = item.copy(url = url, headers = headers, fileName = item.fileName.substringBeforeLast('.') + if (isHls) ".ts" else ".mp4")
		DownloadRepository.update(item.id) { resolved.copy(status = DownloadItem.STATUS_RUNNING) }
		return resolved
	}

	private fun checkSpace(item: DownloadItem) {
		val free = DownloadRepository.freeBytes()
		if (free < MIN_FREE_BYTES) error("Espacio insuficiente: quedan ${humanSize(free)}")
	}

	private fun download(item: DownloadItem) {
		val target = DownloadRepository.file(item)
		if (item.url.substringBefore('?').endsWith(".m3u8", true)) {
			downloadHls(item, target)
		} else {
			downloadFile(item, target)
		}
	}

	private fun downloadFile(item: DownloadItem, target: File) {
		val client = SourcesRuntime.network.client
		val existing = if (target.exists()) target.length() else 0L
		val request = Request.Builder().url(item.url).apply {
			item.headers.forEach { (k, v) -> header(k, v) }
			if (existing > 0) header("Range", "bytes=$existing-")
		}.build()
		client.newCall(request).execute().use { response ->
			if (!response.isSuccessful) error("HTTP ${response.code}")
			val append = response.code == 206 && existing > 0
			val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
			val total = if (length >= 0 && append) length + existing else length
			if (total > 0 && DownloadRepository.freeBytes() < total - existing + MIN_FREE_BYTES / 4) {
				error("Espacio insuficiente para ${humanSize(total)}")
			}
			var written = if (append) existing else 0L
			FileOutputStream(target, append).use { out ->
				response.body.byteStream().use { input ->
					val buffer = ByteArray(256 * 1024)
					var lastNotify = 0L
					while (true) {
						val read = input.read(buffer)
						if (read < 0) break
						out.write(buffer, 0, read)
						written += read
						val now = System.currentTimeMillis()
						if (now - lastNotify > 700) {
							lastNotify = now
							progress(item, written, total)
						}
					}
				}
			}
			if (written == 0L) error("El servidor devolvió un archivo vacío")
			progress(item, written, if (total < 0) written else total)
		}
	}

	private fun downloadHls(item: DownloadItem, target: File) {
		val client = SourcesRuntime.network.client
		fun get(url: String): okhttp3.Response {
			val req = Request.Builder().url(url).apply { item.headers.forEach { (k, v) -> header(k, v) } }.build()
			return client.newCall(req).execute().also { if (!it.isSuccessful) { it.close(); error("HTTP ${it.code} $url") } }
		}
		fun resolve(base: String, ref: String): String = when {
			ref.startsWith("http") -> ref
			ref.startsWith("//") -> Uri.parse(base).scheme + ":" + ref
			ref.startsWith("/") -> Uri.parse(base).let { "${it.scheme}://${it.authority}$ref" }
			else -> base.substringBefore('?').substringBeforeLast('/') + "/" + ref
		}
		var playlistUrl = item.url
		var playlist = get(playlistUrl).use { it.body.string() }
		if (playlist.contains("#EXT-X-STREAM-INF")) {
			val lines = playlist.lines()
			var best: Pair<Long, String>? = null
			lines.forEachIndexed { i, line ->
				if (line.startsWith("#EXT-X-STREAM-INF")) {
					val bw = Regex("""BANDWIDTH=(\d+)""").find(line)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
					val uri = lines.drop(i + 1).firstOrNull { it.isNotBlank() && !it.startsWith("#") }
					if (uri != null && (best == null || bw > best!!.first)) best = bw to uri
				}
			}
			val variant = best?.second ?: error("Empty master playlist")
			playlistUrl = resolve(playlistUrl, variant)
			playlist = get(playlistUrl).use { it.body.string() }
		}
		if (playlist.contains("#EXT-X-KEY") && !playlist.contains("METHOD=NONE")) {
			error("Stream cifrado (AES): descarga no soportada todavía")
		}
		val segments = playlist.lines().filter { it.isNotBlank() && !it.startsWith("#") }.map { resolve(playlistUrl, it) }
		if (segments.isEmpty()) error("Playlist sin segmentos")
		var written = 0L
		FileOutputStream(target, false).use { out ->
			segments.forEachIndexed { index, seg ->
				get(seg).use { resp ->
					resp.body.byteStream().use { input ->
						val buffer = ByteArray(256 * 1024)
						while (true) {
							val read = input.read(buffer)
							if (read < 0) break
							out.write(buffer, 0, read)
							written += read
						}
					}
				}
				val estimated = written / (index + 1) * segments.size
				if (index == 2 && DownloadRepository.freeBytes() < estimated - written + MIN_FREE_BYTES / 4) {
					error("Espacio insuficiente para ${humanSize(estimated)}")
				}
				progress(item, written, estimated)
			}
		}
		if (written == 0L) error("El servidor devolvió un archivo vacío")
		progress(item, written, written)
	}

	private fun progress(item: DownloadItem, bytes: Long, total: Long) {
		DownloadRepository.update(item.id) { it.copy(bytes = bytes, total = total) }
		val running = DownloadRepository.items.value.filter { it.status == DownloadItem.STATUS_RUNNING }
		val queued = DownloadRepository.items.value.count { it.status == DownloadItem.STATUS_QUEUED }
		val percent = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0
		val title = if (running.size > 1) "${running.size} descargas · $queued en cola" else "${item.animeTitle} · ${item.episodeName}"
		val text = if (running.size > 1) running.joinToString(" · ") { r -> "${r.episodeName} ${if (r.total > 0) "${(r.bytes * 100 / r.total)}%" else humanSize(r.bytes)}" }
		else if (total > 0) "$percent %" else humanSize(bytes)
		notify(buildNotification(title, text, percent, total <= 0 || running.size > 1))
	}

	private fun buildNotification(title: String, text: String, progress: Int, indeterminate: Boolean): Notification {
		val manager = getSystemService(NotificationManager::class.java)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			manager.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.downloads), NotificationManager.IMPORTANCE_LOW))
		}
		val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
		return NotificationCompat.Builder(this, CHANNEL)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setContentTitle(title)
			.setContentText(text)
			.setProgress(100, progress, indeterminate)
			.setOngoing(true)
			.setOnlyAlertOnce(true)
			.setContentIntent(open)
			.build()
	}

	private fun notify(notification: Notification) {
		getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
	}

	private fun startForegroundCompat(notification: Notification) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
		} else {
			startForeground(NOTIFICATION_ID, notification)
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		scope.cancel()
	}

	companion object {
		private const val CHANNEL = "downloads"
		private const val NOTIFICATION_ID = 1001
		private const val RESOLVE_TIMEOUT = 90_000L
		private const val MIN_FREE_BYTES = 300L * 1024 * 1024

		fun start(context: Context) {
			val intent = Intent(context, DownloadService::class.java)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
		}
	}
}

fun humanSize(bytes: Long): String = when {
	bytes >= 1L shl 30 -> String.format(java.util.Locale.ROOT, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
	bytes >= 1L shl 20 -> String.format(java.util.Locale.ROOT, "%.0f MB", bytes / (1024.0 * 1024))
	else -> String.format(java.util.Locale.ROOT, "%.0f KB", bytes / 1024.0)
}
