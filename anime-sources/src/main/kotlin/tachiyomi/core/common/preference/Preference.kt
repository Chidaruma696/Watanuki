package tachiyomi.core.common.preference

import android.content.SharedPreferences
import androidx.core.content.edit

/** Minimal preference abstraction the vendored NetworkPreferences expects. */
interface Preference<T> {
	fun get(): T
	fun set(value: T)
}

interface PreferenceStore {
	fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean>
	fun getInt(key: String, defaultValue: Int): Preference<Int>
	fun getString(key: String, defaultValue: String): Preference<String>
}

/** Backed by a plain [SharedPreferences] file. */
class SharedPreferencesStore(private val prefs: SharedPreferences) : PreferenceStore {

	override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = object : Preference<Boolean> {
		override fun get() = prefs.getBoolean(key, defaultValue)
		override fun set(value: Boolean) = prefs.edit { putBoolean(key, value) }
	}

	override fun getInt(key: String, defaultValue: Int): Preference<Int> = object : Preference<Int> {
		override fun get() = prefs.getInt(key, defaultValue)
		override fun set(value: Int) = prefs.edit { putInt(key, value) }
	}

	override fun getString(key: String, defaultValue: String): Preference<String> = object : Preference<String> {
		override fun get() = prefs.getString(key, defaultValue) ?: defaultValue
		override fun set(value: String) = prefs.edit { putString(key, value) }
	}
}
