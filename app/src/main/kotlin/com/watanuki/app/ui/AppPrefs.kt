package com.watanuki.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.watanuki.app.WatanukiApp
import com.watanuki.sources.LoadedSource
import eu.kanade.tachiyomi.animesource.model.SAnime

/** App-wide switches, observable from Compose. */
object AppPrefs {
	private const val FILE = "app"

	var onboardingDone by mutableStateOf(false)
		private set
	var showAdult by mutableStateOf(false)
		private set
	var autoSelectServer by mutableStateOf(true)
		private set
	var parallelDownloads by mutableStateOf(2)
		private set
	/** Keep Android Open notice on Home; hidden once the user dismisses it, can be shown again from Settings. */
	var kaoBannerVisible by mutableStateOf(true)
		private set

	fun init(context: Context) {
		val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
		onboardingDone = p.getBoolean("onboarding_done", false)
		showAdult = p.getBoolean("show_adult", false)
		autoSelectServer = p.getBoolean("auto_server", true)
		parallelDownloads = p.getInt("parallel_downloads", 2)
		kaoBannerVisible = p.getBoolean("kao_banner", true)
	}

	private fun prefs() = WatanukiApp.instance.getSharedPreferences(FILE, Context.MODE_PRIVATE)

	fun setOnboardingDone() { onboardingDone = true; prefs().edit { putBoolean("onboarding_done", true) } }
	fun updateShowAdult(value: Boolean) { showAdult = value; prefs().edit { putBoolean("show_adult", value) } }
	fun updateParallelDownloads(value: Int) { parallelDownloads = value.coerceIn(1, 4); prefs().edit { putInt("parallel_downloads", parallelDownloads) } }
	fun updateKaoBanner(value: Boolean) { kaoBannerVisible = value; prefs().edit { putBoolean("kao_banner", value) } }
	fun updateAutoSelectServer(value: Boolean) { autoSelectServer = value; prefs().edit { putBoolean("auto_server", value) } }

	private val adultWords = listOf("hentai", "ecchi", "+18", "18+", "adulto", "adult", "yaoi hard", "smut", "erótico", "erotico", "porn")

	/** True when a title should stay hidden while adult content is off. */
	fun isAdult(anime: SAnime, source: LoadedSource): Boolean {
		if (source.isNsfw) return true
		val genres = anime.genre?.lowercase().orEmpty()
		val title = anime.title.lowercase()
		return adultWords.any { it in genres } || title.contains("hentai")
	}
}
