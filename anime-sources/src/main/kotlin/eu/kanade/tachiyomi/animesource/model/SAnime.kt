@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

import java.io.Serializable

interface SAnime : Serializable {

	var url: String
	var title: String
	var artist: String?
	var author: String?
	var description: String?

	/** Comma-separated list of genres. */
	var genre: String?

	var status: Int
	var thumbnail_url: String?
	var update_strategy: AnimeUpdateStrategy
	var initialized: Boolean

	fun getGenres(): List<String>? = genre?.split(", ")?.map { it.trim() }?.filterNot { it.isBlank() }?.distinct()

	fun copyFrom(other: SAnime) {
		if (other.author != null) author = other.author
		if (other.artist != null) artist = other.artist
		if (other.description != null) description = other.description
		if (other.genre != null) genre = other.genre
		if (other.thumbnail_url != null) thumbnail_url = other.thumbnail_url
		status = other.status
		update_strategy = other.update_strategy
		if (!initialized) initialized = other.initialized
	}

	companion object {
		const val UNKNOWN = 0
		const val ONGOING = 1
		const val COMPLETED = 2
		const val LICENSED = 3
		const val PUBLISHING_FINISHED = 4
		const val CANCELLED = 5
		const val ON_HIATUS = 6

		fun create(): SAnime = SAnimeImpl()
	}
}

class SAnimeImpl : SAnime {
	override lateinit var url: String
	override lateinit var title: String
	override var artist: String? = null
	override var author: String? = null
	override var description: String? = null
	override var genre: String? = null
	override var status: Int = SAnime.UNKNOWN
	override var thumbnail_url: String? = null
	override var update_strategy: AnimeUpdateStrategy = AnimeUpdateStrategy.ALWAYS_UPDATE
	override var initialized: Boolean = false

	override fun toString(): String = "SAnime(title=${if (::title.isInitialized) title else "?"}, url=${if (::url.isInitialized) url else "?"})"
}
