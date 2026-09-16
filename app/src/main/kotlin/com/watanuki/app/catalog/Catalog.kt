package com.watanuki.app.catalog

import android.util.Log
import kotlinx.coroutines.CancellationException

/** The catalogue the app talks to: AniList first, MyAnimeList (Jikan) when AniList is out. */
object Catalog {

	private const val TAG = "Catalog"
	private val memo = HashMap<String, CatalogAnime>()

	suspend fun search(query: String, sfw: Boolean): List<CatalogAnime> = try {
		AniList.search(query, sfw = sfw)
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		Log.w(TAG, "AniList failed, falling back to Jikan: ${e.message}")
		Jikan.search(query, sfw = sfw)
	}

	/** The entry with its relations, from whichever backend owns it; memoised for the session. */
	suspend fun full(id: Int, backend: Backend): CatalogAnime {
		val key = "$backend:$id"
		synchronized(memo) { memo[key] }?.let { return it }
		val entry = when (backend) {
			Backend.ANILIST -> AniList.full(id)
			Backend.MAL -> Jikan.full(id)
		}
		synchronized(memo) { memo[key] = entry }
		return entry
	}

	/** Same entry, relations guaranteed. */
	suspend fun withRelations(anime: CatalogAnime): CatalogAnime = if (anime.relationsKnown) anime else full(anime.id, anime.backend)
}
