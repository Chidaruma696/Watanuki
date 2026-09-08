package eu.kanade.tachiyomi.animesource.online

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.awaitSuccess
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * Real implementation of the extensions-lib 14 [AnimeHttpSource]: an online source that
 * builds requests and parses responses. Suspend entry points do the work; the deprecated
 * RxJava ones remain for sources that still override them.
 */
@Suppress("DEPRECATION", "unused")
abstract class AnimeHttpSource : AnimeCatalogueSource {

	protected val network: NetworkHelper by injectLazy()

	/** Base url of the website without the trailing slash, like: https://example.com */
	abstract val baseUrl: String

	/** Version id used to generate the source id. Bump when the source's id must change. */
	open val versionId: Int = 1

	override val id: Long by lazy { generateId(name, lang, versionId) }

	/** Headers used by default in every request. */
	val headers: Headers by lazy { headersBuilder().build() }

	/** Default network client for this source. */
	open val client: OkHttpClient
		get() = network.client

	protected fun generateId(name: String, lang: String, versionId: Int): Long {
		val key = "${name.lowercase()}/$lang/$versionId"
		val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
		return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
	}

	protected open fun headersBuilder(): Headers.Builder = Headers.Builder()
		.add("User-Agent", network.defaultUserAgentProvider())

	override fun toString(): String = "$name (${lang.uppercase()})"

	// ---------------------------------------------------------------- popular

	override suspend fun getPopularAnime(page: Int): AnimesPage =
		client.newCall(popularAnimeRequest(page)).awaitSuccess().use { popularAnimeParse(it) }

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getPopularAnime"))
	override fun fetchPopularAnime(page: Int): Observable<AnimesPage> =
		client.newCall(popularAnimeRequest(page)).asObservableSuccess().map { popularAnimeParse(it) }

	protected abstract fun popularAnimeRequest(page: Int): Request
	protected abstract fun popularAnimeParse(response: Response): AnimesPage

	// ---------------------------------------------------------------- search

	override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
		client.newCall(searchAnimeRequest(page, query, filters)).awaitSuccess().use { searchAnimeParse(it) }

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getSearchAnime"))
	override fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage> =
		client.newCall(searchAnimeRequest(page, query, filters)).asObservableSuccess().map { searchAnimeParse(it) }

	protected abstract fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request
	protected abstract fun searchAnimeParse(response: Response): AnimesPage

	// ---------------------------------------------------------------- latest

	override suspend fun getLatestUpdates(page: Int): AnimesPage =
		client.newCall(latestUpdatesRequest(page)).awaitSuccess().use { latestUpdatesParse(it) }

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getLatestUpdates"))
	override fun fetchLatestUpdates(page: Int): Observable<AnimesPage> =
		client.newCall(latestUpdatesRequest(page)).asObservableSuccess().map { latestUpdatesParse(it) }

	protected abstract fun latestUpdatesRequest(page: Int): Request
	protected abstract fun latestUpdatesParse(response: Response): AnimesPage

	// ---------------------------------------------------------------- details

	override suspend fun getAnimeDetails(anime: SAnime): SAnime =
		client.newCall(animeDetailsRequest(anime)).awaitSuccess().use { animeDetailsParse(it) }

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getAnimeDetails"))
	override fun fetchAnimeDetails(anime: SAnime): Observable<SAnime> =
		client.newCall(animeDetailsRequest(anime)).asObservableSuccess().map { animeDetailsParse(it) }

	open fun animeDetailsRequest(anime: SAnime): Request = GET(baseUrl + anime.url, headers)

	protected abstract fun animeDetailsParse(response: Response): SAnime

	// ---------------------------------------------------------------- episodes

	override suspend fun getEpisodeList(anime: SAnime): List<SEpisode> {
		if (anime.status == SAnime.LICENSED) {
			throw LicensedAnimeEpisodesException()
		}
		return client.newCall(episodeListRequest(anime)).awaitSuccess().use { episodeListParse(it) }
	}

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getEpisodeList"))
	override fun fetchEpisodeList(anime: SAnime): Observable<List<SEpisode>> {
		if (anime.status == SAnime.LICENSED) {
			return Observable.error(LicensedAnimeEpisodesException())
		}
		return client.newCall(episodeListRequest(anime)).asObservableSuccess().map { episodeListParse(it) }
	}

	protected open fun episodeListRequest(anime: SAnime): Request = GET(baseUrl + anime.url, headers)

	protected abstract fun episodeListParse(response: Response): List<SEpisode>

	// ---------------------------------------------------------------- videos

	override suspend fun getVideoList(episode: SEpisode): List<Video> =
		client.newCall(videoListRequest(episode)).awaitSuccess().use { videoListParse(it).sort() }

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getVideoList"))
	override fun fetchVideoList(episode: SEpisode): Observable<List<Video>> =
		client.newCall(videoListRequest(episode)).asObservableSuccess().map { videoListParse(it).sort() }

	open fun fetchVideoUrl(video: Video): Observable<String> =
		client.newCall(videoUrlRequest(video)).asObservableSuccess().map { videoUrlParse(it) }

	protected open fun videoListRequest(episode: SEpisode): Request = GET(baseUrl + episode.url, headers)

	protected open fun videoListParse(response: Response): List<Video> =
		throw UnsupportedOperationException("Not used")

	/** Sorts the video list; sources override it to honour quality preferences. */
	protected open fun List<Video>.sort(): List<Video> = this

	protected open fun videoUrlRequest(video: Video): Request = GET(video.url, headers)

	protected open fun videoUrlParse(response: Response): String =
		throw UnsupportedOperationException("Not used")

	// ---------------------------------------------------------------- urls

	fun SEpisode.setUrlWithoutDomain(url: String) {
		this.url = getUrlWithoutDomain(url)
	}

	fun SAnime.setUrlWithoutDomain(url: String) {
		this.url = getUrlWithoutDomain(url)
	}

	private fun getUrlWithoutDomain(orig: String): String {
		return try {
			val uri = URI(orig.replace(" ", "%20"))
			var out = uri.path
			if (uri.query != null) out += "?" + uri.query
			if (uri.fragment != null) out += "#" + uri.fragment
			out
		} catch (_: URISyntaxException) {
			orig
		}
	}

	/** Absolute url of the anime page, for opening in a browser. */
	open fun getAnimeUrl(anime: SAnime): String = animeDetailsRequest(anime).url.toString()

	/** Absolute url of the episode page. */
	open fun getEpisodeUrl(episode: SEpisode): String = episodeListRequest(SAnime.create().apply { url = episode.url }).url.toString()

	/** Called before inserting a new episode in the library; lets sources fix names or numbers. */
	open fun prepareNewEpisode(episode: SEpisode, anime: SAnime) {}

	override fun getFilterList(): AnimeFilterList = AnimeFilterList()
}

class LicensedAnimeEpisodesException : RuntimeException("Licensed - No episodes to show")
