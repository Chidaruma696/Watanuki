package com.watanuki.app.catalog

/** Where an entry came from; ids only mean something within their own backend. */
enum class Backend { ANILIST, MAL }

/** A prequel, sequel, side story… of an entry, as "kind" (lowercase) plus the other entry's id. */
data class CatalogRelation(val kind: String, val id: Int)

/**
 * One anime as the catalogue knows it, whichever backend answered. Everything the app needs to
 * show a card, match the entry against the streaming sites and walk its seasons.
 */
data class CatalogAnime(
	val id: Int,
	val backend: Backend,
	val title: String,
	val titleEnglish: String? = null,
	val titleJapanese: String? = null,
	val synonyms: List<String> = emptyList(),
	val cover: String? = null,
	val synopsis: String? = null,
	val episodes: Int? = null,
	val year: Int? = null,
	val type: String? = null,
	val status: String? = null,
	val score: Double? = null,
	/** ISO date the entry started airing; "9999" when unknown so it sorts last. */
	val airedFrom: String = "9999",
	val relations: List<CatalogRelation> = emptyList(),
	/** False when the backend needs a second call to reveal the relations (Jikan does). */
	val relationsKnown: Boolean = false,
) {
	/** Every name the catalogue knows this entry by, main title first. */
	val allTitles: List<String>
		get() = (listOf(title) + listOfNotNull(titleEnglish, titleJapanese) + synonyms).filter { it.isNotBlank() }.distinct()

	fun related(kind: String): List<Int> = relations.filter { it.kind == kind }.map { it.id }
}
