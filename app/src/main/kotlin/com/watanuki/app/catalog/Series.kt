package com.watanuki.app.catalog

import kotlinx.coroutines.CancellationException

/**
 * A show the way people think of it: every catalogue entry chained by prequel/sequel, in
 * broadcast order. "Fate/Zero" and "Fate/Zero 2nd Season" are one series with two seasons.
 */
data class CatalogSeries(val seasons: List<CatalogAnime>) {
	val main: CatalogAnime get() = seasons.first()
	val id: Int get() = main.id
	val backend: Backend get() = main.backend
	val title: String get() = main.title
}

object SeriesGrouper {

	/** Types worth listing; music videos and the like never are. */
	private val listed = setOf("TV", "ONA", "OVA", "Movie", "Special", "TV Special")

	/**
	 * Collapses search hits into series: hits that point at each other as prequel/sequel land in
	 * the same group. Relations are fetched only when the backend did not send them along;
	 * [onProgress] receives the groups gathered so far, so the list can fill in meanwhile.
	 */
	suspend fun group(hits: List<CatalogAnime>, onProgress: suspend (List<CatalogSeries>) -> Unit = {}): List<CatalogSeries> {
		val wanted = hits.filter { it.type == null || it.type in listed }
		val full = LinkedHashMap<Int, CatalogAnime>()
		val parent = HashMap<Int, Int>()
		fun find(x: Int): Int { var r = x; while (parent[r] != null && parent[r] != r) r = parent[r]!!; parent[x] = r; return r }
		fun union(a: Int, b: Int) { parent.putIfAbsent(a, a); parent.putIfAbsent(b, b); val ra = find(a); val rb = find(b); if (ra != rb) parent[rb] = ra }

		for (hit in wanted) {
			val entry = try { Catalog.withRelations(hit) } catch (e: CancellationException) { throw e } catch (e: Throwable) { hit }
			full[entry.id] = entry
			parent.putIfAbsent(entry.id, entry.id)
			(entry.related("sequel") + entry.related("prequel")).forEach { union(entry.id, it) }
			if (!hit.relationsKnown) onProgress(build(full, ::find))
		}
		return build(full, ::find)
	}

	/** Grows a series along its prequel/sequel chain until nothing new turns up (bounded). */
	suspend fun expand(series: CatalogSeries, maxSeasons: Int = 20): CatalogSeries {
		val known = LinkedHashMap<Int, CatalogAnime>()
		series.seasons.forEach { known[it.id] = it }
		val queue = ArrayDeque(series.seasons.map { it.id })
		while (queue.isNotEmpty() && known.size < maxSeasons) {
			val id = queue.removeFirst()
			val entry = try { Catalog.full(id, series.backend) } catch (e: CancellationException) { throw e } catch (e: Throwable) { continue }
			known[id] = entry
			(entry.related("prequel") + entry.related("sequel")).forEach { next ->
				if (next !in known && next !in queue) queue.addLast(next)
			}
		}
		return CatalogSeries(known.values.filter { it.type == null || it.type in listed }.sortedBy { it.airedFrom })
	}

	private fun build(full: Map<Int, CatalogAnime>, find: (Int) -> Int): List<CatalogSeries> {
		val groups = LinkedHashMap<Int, MutableList<CatalogAnime>>()
		full.values.forEach { groups.getOrPut(find(it.id)) { mutableListOf() } += it }
		return groups.values.map { CatalogSeries(it.sortedBy { a -> a.airedFrom }) }
	}
}
