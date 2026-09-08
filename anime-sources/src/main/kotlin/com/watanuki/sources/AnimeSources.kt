package com.watanuki.sources

import android.util.Log
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import eu.kanade.tachiyomi.animesource.AnimeSourceFactory

/** A loaded source plus the metadata of the extension it came from. */
data class LoadedSource(
	val source: AnimeCatalogueSource,
	val entry: SourceEntry,
) {
	val id: Long get() = source.id
	val name: String get() = source.name
	val lang: String get() = source.lang
	val isNsfw: Boolean get() = entry.isNsfw
}

/**
 * Registry of every source compiled into the app. Instantiation is lazy and failures are
 * isolated: a source that throws in its constructor is logged and skipped, never fatal.
 */
object AnimeSources {

	val entries: List<SourceEntry> get() = GeneratedSources.entries

	val all: List<LoadedSource> by lazy { entries.flatMap(::load) }

	val failed: Map<SourceEntry, Throwable> get() = failures

	private val failures = LinkedHashMap<SourceEntry, Throwable>()

	fun byId(id: Long): LoadedSource? = all.firstOrNull { it.id == id }

	private fun load(entry: SourceEntry): List<LoadedSource> = try {
		val instance = Class.forName(entry.className).getDeclaredConstructor().newInstance()
		val sources: List<AnimeSource> = when (instance) {
			is AnimeSourceFactory -> instance.createSources()
			is AnimeSource -> listOf(instance)
			else -> error("${entry.className} is not an AnimeSource")
		}
		sources.filterIsInstance<AnimeCatalogueSource>().map { LoadedSource(it, entry) }
	} catch (e: Throwable) {
		Log.w("AnimeSources", "Could not load ${entry.name} (${entry.className})", e)
		failures[entry] = e
		emptyList()
	}
}
