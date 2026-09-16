package com.watanuki.app.catalog

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable private data class AlTitle(val romaji: String? = null, val english: String? = null, val native: String? = null)
@Serializable private data class AlDate(val year: Int? = null, val month: Int? = null, val day: Int? = null)
@Serializable private data class AlCover(val large: String? = null, val extraLarge: String? = null)
@Serializable private data class AlNode(val id: Int, val type: String? = null)
@Serializable private data class AlEdge(val relationType: String? = null, val node: AlNode? = null)
@Serializable private data class AlRelations(val edges: List<AlEdge> = emptyList())
@Serializable private data class AlMedia(
	val id: Int,
	val idMal: Int? = null,
	val title: AlTitle? = null,
	val synonyms: List<String> = emptyList(),
	val coverImage: AlCover? = null,
	val description: String? = null,
	val episodes: Int? = null,
	val seasonYear: Int? = null,
	val format: String? = null,
	val status: String? = null,
	val averageScore: Int? = null,
	val startDate: AlDate? = null,
	val relations: AlRelations? = null,
)
@Serializable private data class AlPage(val media: List<AlMedia> = emptyList())
@Serializable private data class AlData(val Page: AlPage? = null, val Media: AlMedia? = null)
@Serializable private data class AlError(val message: String? = null)
@Serializable private data class AlResponse(val data: AlData? = null, val errors: List<AlError> = emptyList())

/**
 * AniList (graphql.anilist.co), the main catalogue: first-party API, no key for public reads,
 * and a search answers with every hit's relations in the same reply, so grouping seasons costs
 * one request. Rate limit is per minute; calls are spaced and 429s wait what the server says.
 */
object AniList {

	private const val URL = "https://graphql.anilist.co"
	private const val TAG = "AniList"
	private const val SPACING_MS = 700L
	private const val RETRIES = 2

	private const val FIELDS = """
		id idMal title { romaji english native } synonyms
		coverImage { large extraLarge } description(asHtml: false)
		episodes seasonYear format status averageScore startDate { year month day }
		relations { edges { relationType node { id type } } }
	"""
	private const val SEARCH = "query(\$s: String, \$n: Int, \$adult: Boolean) { Page(perPage: \$n) { media(search: \$s, type: ANIME, isAdult: \$adult, sort: SEARCH_MATCH) { $FIELDS } } }"
	private const val ONE = "query(\$id: Int) { Media(id: \$id, type: ANIME) { $FIELDS } }"

	private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }
	private val client: OkHttpClient by lazy {
		OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
	}
	private val gate = Mutex()
	private var lastCall = 0L
	private val memo = HashMap<String, AlResponse>()

	suspend fun search(query: String, limit: Int = 20, sfw: Boolean = true): List<CatalogAnime> {
		val vars = buildJsonObject { put("s", query.trim()); put("n", limit); if (sfw) put("adult", false) }
		return post(SEARCH, vars).data?.Page?.media.orEmpty().map { it.toCatalog() }
	}

	suspend fun full(id: Int): CatalogAnime =
		post(ONE, buildJsonObject { put("id", id) }).data?.Media?.toCatalog() ?: error("AniList: entrada $id no encontrada")

	private fun AlMedia.toCatalog(): CatalogAnime {
		val start = startDate
		val aired = if (start?.year != null) String.format(java.util.Locale.ROOT, "%04d-%02d-%02d", start.year, start.month ?: 1, start.day ?: 1) else "9999"
		return CatalogAnime(
			id = id,
			backend = Backend.ANILIST,
			title = title?.romaji ?: title?.english ?: title?.native ?: "?",
			titleEnglish = title?.english,
			titleJapanese = title?.native,
			synonyms = synonyms,
			cover = coverImage?.extraLarge ?: coverImage?.large,
			synopsis = description?.replace(Regex("<br\\s*/?>"), "\n")?.replace(Regex("<[^>]+>"), "")?.trim(),
			episodes = episodes,
			year = seasonYear ?: start?.year,
			type = when (format) { "TV", "TV_SHORT" -> "TV"; "MOVIE" -> "Movie"; "SPECIAL" -> "Special"; "OVA" -> "OVA"; "ONA" -> "ONA"; "MUSIC" -> "Music"; else -> format },
			status = when (status) { "FINISHED" -> "Finished Airing"; "RELEASING" -> "Currently Airing"; "NOT_YET_RELEASED" -> "Not yet aired"; else -> status },
			score = averageScore?.let { it / 10.0 },
			airedFrom = aired,
			relations = relations?.edges.orEmpty().mapNotNull { e ->
				val node = e.node ?: return@mapNotNull null
				if (node.type != "ANIME" || e.relationType == null) null else CatalogRelation(e.relationType.lowercase(), node.id)
			},
			relationsKnown = true,
		)
	}

	private suspend fun post(query: String, variables: JsonObject): AlResponse = withContext(Dispatchers.IO) {
		val payload = buildJsonObject { put("query", query); put("variables", variables) }.toString()
		synchronized(memo) { memo[payload] }?.let { return@withContext it }
		repeat(RETRIES + 1) { attempt ->
			gate.withLock {
				val wait = SPACING_MS - (System.currentTimeMillis() - lastCall)
				if (wait > 0) delay(wait)
				lastCall = System.currentTimeMillis()
			}
			val request = Request.Builder().url(URL)
				.header("Accept", "application/json")
				.post(payload.toRequestBody("application/json".toMediaType()))
				.build()
			val (code, retryAfter, body) = client.newCall(request).execute().use { r -> Triple(r.code, r.header("Retry-After")?.toLongOrNull(), r.body.string()) }
			if (code == 200) {
				val parsed = json.decodeFromString<AlResponse>(body)
				parsed.errors.firstOrNull()?.message?.let { error("AniList: $it") }
				synchronized(memo) { memo[payload] = parsed }
				return@withContext parsed
			}
			if (code != 429 && code < 500) error("AniList: HTTP $code")
			Log.w(TAG, "HTTP $code, retry ${attempt + 1}")
			delay(((retryAfter ?: 2L) * 1000L).coerceAtMost(15_000L))
		}
		error("AniList no responde ahora mismo")
	}
}
