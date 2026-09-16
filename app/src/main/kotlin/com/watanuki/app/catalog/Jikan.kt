package com.watanuki.app.catalog

import android.util.Log
import com.watanuki.app.WatanukiApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

@Serializable private data class JikanAnime(
	val mal_id: Int,
	val title: String,
	val title_english: String? = null,
	val title_japanese: String? = null,
	val title_synonyms: List<String> = emptyList(),
	val images: JikanImages? = null,
	val synopsis: String? = null,
	val episodes: Int? = null,
	val year: Int? = null,
	val type: String? = null,
	val status: String? = null,
	val score: Double? = null,
	val aired: JikanAired? = null,
	val relations: List<JikanRelation> = emptyList(),
)
@Serializable private data class JikanImages(val jpg: JikanImage? = null)
@Serializable private data class JikanImage(val image_url: String? = null, val large_image_url: String? = null)
@Serializable private data class JikanAired(val from: String? = null)
@Serializable private data class JikanRelation(val relation: String, val entry: List<JikanRelEntry> = emptyList())
@Serializable private data class JikanRelEntry(val mal_id: Int, val type: String)
@Serializable private data class JikanPage(val data: List<JikanAnime> = emptyList())
@Serializable private data class JikanOne(val data: JikanAnime)

/**
 * MyAnimeList through Jikan (api.jikan.moe), the fallback catalogue: no key, but 3 requests a
 * second, one extra call per entry to learn its relations, and it goes down whenever MAL does.
 * Calls are spaced out and cached on disk.
 */
object Jikan {

	private const val BASE = "https://api.jikan.moe/v4"
	private const val TAG = "Jikan"
	private const val SPACING_MS = 400L
	private const val RETRIES = 3

	private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
	private val client: OkHttpClient by lazy {
		OkHttpClient.Builder()
			.cache(Cache(File(WatanukiApp.instance.cacheDir, "jikan"), 20L * 1024 * 1024))
			.connectTimeout(15, TimeUnit.SECONDS)
			.readTimeout(20, TimeUnit.SECONDS)
			.build()
	}
	private val gate = Mutex()
	private var lastCall = 0L

	suspend fun search(query: String, limit: Int = 20, sfw: Boolean = true): List<CatalogAnime> {
		val q = java.net.URLEncoder.encode(query.trim(), "UTF-8")
		val url = "$BASE/anime?q=$q&limit=$limit${if (sfw) "&sfw=true" else ""}"
		return json.decodeFromString<JikanPage>(get(url)).data.map { it.toCatalog(full = false) }
	}

	/** The entry with its relations. */
	suspend fun full(id: Int): CatalogAnime =
		json.decodeFromString<JikanOne>(get("$BASE/anime/$id/full")).data.toCatalog(full = true)

	private fun JikanAnime.toCatalog(full: Boolean) = CatalogAnime(
		id = mal_id,
		backend = Backend.MAL,
		title = title,
		titleEnglish = title_english,
		titleJapanese = title_japanese,
		synonyms = title_synonyms,
		cover = images?.jpg?.large_image_url ?: images?.jpg?.image_url,
		synopsis = synopsis?.substringBefore("[Written by MAL Rewrite]")?.trim(),
		episodes = episodes,
		year = year,
		type = type,
		status = status,
		score = score,
		airedFrom = aired?.from ?: "9999",
		relations = relations.flatMap { r -> r.entry.filter { it.type == "anime" }.map { CatalogRelation(r.relation.lowercase().replace(' ', '_'), it.mal_id) } },
		relationsKnown = full,
	)

	private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
		repeat(RETRIES + 1) { attempt ->
			// one request at a time, spaced out, so the rate limit is never hit on purpose
			gate.withLock {
				val wait = SPACING_MS - (System.currentTimeMillis() - lastCall)
				if (wait > 0) delay(wait)
				lastCall = System.currentTimeMillis()
			}
			val body = client.newCall(Request.Builder().url(url).build()).execute().use { r ->
				when {
					r.isSuccessful -> r.body.string()
					// 429 is the rate limit, 5xx is Jikan losing MAL for a moment: both deserve another go
					r.code == 429 || r.code >= 500 -> null
					else -> error("MyAnimeList: HTTP ${r.code}")
				}
			}
			if (body != null) return@withContext body
			Log.w(TAG, "no answer, retry ${attempt + 1}")
			delay(1200L * (attempt + 1))
		}
		error("MyAnimeList no responde ahora mismo")
	}
}
