package com.watanuki.app.ui

import eu.kanade.tachiyomi.animesource.model.Video

/**
 * Servers describe themselves in free text ("YourUpload: 720p", "Voe HD"), so the quality has
 * to be read out of the label. With a number in hand the app can always offer the best server
 * first and walk down the list when one fails.
 */
object VideoQuality {

	/** "Whatever is best" — the default, and what the picker shows first. */
	const val AUTO = "auto"

	/** Offered in Settings, best to worst. */
	val options = listOf(AUTO, "1080", "720", "480", "360")

	private val number = Regex("""(\d{3,4})\s*p?""")

	/** Vertical resolution in pixels; 0 when the label says nothing useful. */
	fun resolution(video: Video): Int {
		val label = video.quality.lowercase()
		number.find(label)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 144..4320) return it }
		return when {
			"fullhd" in label || "full hd" in label -> 1080
			"hd" in label -> 720
			"sd" in label -> 480
			"low" in label -> 360
			else -> 0
		}
	}

	/** The server's own name, to show next to the quality. */
	fun server(video: Video): String {
		val head = video.quality.substringBefore(':', "").trim()
		if (head.isNotBlank() && !head.first().isDigit()) return head
		return (video.videoUrl ?: video.url).substringAfter("://").substringBefore('/').removePrefix("www.")
	}

	/** "1080p" when the label has a number in it, the raw label otherwise. */
	fun label(video: Video): String {
		val res = resolution(video)
		return if (res > 0) "${res}p" else video.quality
	}

	/** Best first: with a concrete preference the servers that match it lead, the rest descend. */
	fun comparator(preferred: String = AppPrefs.preferredQuality): Comparator<Video> {
		val wanted = preferred.toIntOrNull() ?: return compareByDescending { resolution(it) }
		return compareByDescending<Video> { resolution(it) == wanted }.thenByDescending { resolution(it) }
	}

	fun order(videos: List<Video>, preferred: String = AppPrefs.preferredQuality): List<Video> = videos.sortedWith(comparator(preferred))
}
