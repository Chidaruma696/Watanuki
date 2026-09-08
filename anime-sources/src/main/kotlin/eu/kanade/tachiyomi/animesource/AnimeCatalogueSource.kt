package eu.kanade.tachiyomi.animesource

import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

/** extensions-lib 14 shape: suspend API with defaults bridged from the legacy RxJava calls. */
interface AnimeCatalogueSource : AnimeSource {

	/** An ISO 639-1 compliant language code (two letters in lower case). */
	val lang: String

	/** Whether the source has support for latest updates. */
	val supportsLatest: Boolean

	suspend fun getPopularAnime(page: Int): AnimesPage {
		@Suppress("DEPRECATION")
		return fetchPopularAnime(page).awaitSingle()
	}

	suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
		@Suppress("DEPRECATION")
		return fetchSearchAnime(page, query, filters).awaitSingle()
	}

	suspend fun getLatestUpdates(page: Int): AnimesPage {
		@Suppress("DEPRECATION")
		return fetchLatestUpdates(page).awaitSingle()
	}

	/** Returns the list of filters for the source. */
	fun getFilterList(): AnimeFilterList

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getPopularAnime"))
	fun fetchPopularAnime(page: Int): Observable<AnimesPage>

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getSearchAnime"))
	fun fetchSearchAnime(page: Int, query: String, filters: AnimeFilterList): Observable<AnimesPage>

	@Deprecated("Use the non-RxJava API instead", ReplaceWith("getLatestUpdates"))
	fun fetchLatestUpdates(page: Int): Observable<AnimesPage>
}
