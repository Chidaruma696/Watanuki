@file:Suppress("PropertyName")

package eu.kanade.tachiyomi.animesource.model

import java.io.Serializable

interface SEpisode : Serializable {

	var url: String
	var name: String
	var date_upload: Long
	var episode_number: Float
	var scanlator: String?

	fun copyFrom(other: SEpisode) {
		name = other.name
		url = other.url
		date_upload = other.date_upload
		episode_number = other.episode_number
		scanlator = other.scanlator
	}

	companion object {
		fun create(): SEpisode = SEpisodeImpl()
	}
}

class SEpisodeImpl : SEpisode {
	override lateinit var url: String
	override lateinit var name: String
	override var date_upload: Long = 0
	override var episode_number: Float = -1f
	override var scanlator: String? = null

	override fun toString(): String = "SEpisode(name=${if (::name.isInitialized) name else "?"}, number=$episode_number)"
}
