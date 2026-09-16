package com.watanuki.app.catalog

import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.FeedAnime
import com.watanuki.app.ui.SourcePrefs
import com.watanuki.sources.AnimeSources
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer

/**
 * Finds a MAL entry in the streaming sources. Sites name things their own way ("Fate/Zero 2"
 * for MAL's "Fate/Zero 2nd Season"), so titles are boiled down to a comparable key before
 * matching, and every name MAL knows is tried.
 */
object SourceMatch {

	private val ordinal = Regex("""\b(\d+)(?:st|nd|rd|th)\s+season\b""")
	private val seasonWord = Regex("""\b(?:season|temporada|saison|staffel|part|parte|cour)\s*(\d+)\b""")
	private val shortSeason = Regex("""\bs(\d{1,2})\b""")
	private val roman = Regex("""\b(ii|iii|iv|v|vi)$""")
	private val trailingNumber = Regex("""^(.+\D)\s(\d)$""")
	private val romanValue = mapOf("ii" to 2, "iii" to 3, "iv" to 4, "v" to 5, "vi" to 6)

	/** Lowercase, no accents, no punctuation, and every way of saying "season N" spelled the same. */
	fun key(raw: String): String {
		var s = Normalizer.normalize(raw.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
		s = s.replace(Regex("[^a-z0-9]+"), " ").trim()
		s = ordinal.replace(s) { "season ${it.groupValues[1]}" }
		s = seasonWord.replace(s) { "season ${it.groupValues[1]}" }
		s = shortSeason.replace(s) { "season ${it.groupValues[1].toInt()}" }
		s = roman.replace(s) { "season ${romanValue[it.groupValues[1]]}" }
		// "fate zero 2" means the second season, "mob psycho 100" does not
		if ("season" !in s) trailingNumber.find(s)?.let { s = "${it.groupValues[1].trim()} season ${it.groupValues[2]}" }
		return s.replace(Regex("\\s+"), " ").trim()
	}

	/** One source's copy of the entry, plus how sure the match is (higher is better). */
	data class Hit(val feed: FeedAnime, val confidence: Int)

	/** Looks the entry up in every usable source at once; best matches first. */
	suspend fun find(anime: CatalogAnime, onlyEnabled: Boolean = true): List<Hit> = withContext(Dispatchers.IO) {
		val keys = anime.allTitles.map(::key).filter { it.length >= 3 }.distinct()
		if (keys.isEmpty()) return@withContext emptyList()
		val enabled = SourcePrefs.enabledIds()
		val sources = AnimeSources.all.filter { (!onlyEnabled || it.id in enabled) && (AppPrefs.showAdult || !it.isNsfw) }
		val semaphore = Semaphore(PARALLEL)
		coroutineScope {
			sources.map { src ->
				async {
					semaphore.withPermit {
						try {
							withTimeoutOrNull(SEARCH_TIMEOUT) { searchIn(src, anime, keys) }
						} catch (e: CancellationException) {
							throw e
						} catch (e: Throwable) {
							null
						}
					}
				}
			}.mapNotNull { it.await() }
		}.sortedByDescending { it.confidence }
	}

	private suspend fun searchIn(src: LoadedSource, anime: CatalogAnime, keys: List<String>): Hit? {
		// the main title first, the English one as a second try; that covers nearly every site
		val queries = listOf(anime.title, anime.titleEnglish).filterNotNull().distinct().take(2)
		var best: Hit? = null
		for (q in queries) {
			val results = src.source.getSearchAnime(1, q, src.source.getFilterList()).animes
			val hit = pick(results, keys, src)
			if (hit != null && (best == null || hit.confidence > best.confidence)) best = hit
			if (best != null && best.confidence >= EXACT) break
		}
		return best
	}

	private fun pick(results: List<SAnime>, keys: List<String>, src: LoadedSource): Hit? {
		var best: Hit? = null
		for (r in results) {
			if (!AppPrefs.showAdult && AppPrefs.isAdult(r, src)) continue
			val k = key(r.title)
			val confidence = when {
				k in keys -> EXACT
				keys.any { (it.startsWith("$k ") || k.startsWith("$it ")) && minOf(it.length, k.length) >= maxOf(it.length, k.length) * 0.7 } -> CLOSE
				else -> 0
			}
			if (confidence > (best?.confidence ?: 0)) best = Hit(FeedAnime(r, src), confidence)
			if (confidence == EXACT) break
		}
		return best
	}

	private const val EXACT = 2
	private const val CLOSE = 1
	private const val PARALLEL = 6
	private const val SEARCH_TIMEOUT = 15_000L
}
