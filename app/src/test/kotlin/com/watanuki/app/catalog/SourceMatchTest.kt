package com.watanuki.app.catalog

import org.junit.Assert.assertEquals
import org.junit.Test

/** The key has one job: make MAL's name and each site's name for the same season collide. */
class SourceMatchTest {

	private fun k(s: String) = SourceMatch.key(s)

	@Test fun `season written three ways is one key`() {
		assertEquals("fate zero season 2", k("Fate/Zero 2nd Season"))
		assertEquals("fate zero season 2", k("Fate/Zero 2"))
		assertEquals("fate zero season 2", k("Fate Zero Temporada 2"))
		assertEquals("fate zero season 2", k("Fate/Zero S2"))
	}

	@Test fun `first season keeps its plain name`() {
		assertEquals("fate zero", k("Fate/Zero"))
		assertEquals("one piece", k("One Piece"))
	}

	@Test fun `a number that is part of the title is left alone`() {
		assertEquals("mob psycho 100", k("Mob Psycho 100"))
	}

	@Test fun `ordinals, words and roman numerals agree`() {
		assertEquals("boku no hero academia season 4", k("Boku no Hero Academia 4th Season"))
		assertEquals("boku no hero academia season 4", k("Boku no Hero Academia Season 4"))
		assertEquals("overlord season 2", k("Overlord II"))
		assertEquals("overlord season 2", k("Overlord 2"))
		assertEquals("overlord season 3", k("Overlord III"))
	}

	@Test fun `accents and punctuation do not matter`() {
		assertEquals("pokemon", k("Pokémon"))
		assertEquals("re zero kara hajimeru isekai seikatsu", k("Re:Zero kara Hajimeru Isekai Seikatsu"))
		assertEquals("kimetsu no yaiba yuukaku hen", k("Kimetsu no Yaiba: Yuukaku-hen"))
	}

	@Test fun `a titled final season is not mistaken for a numbered one`() {
		assertEquals("shingeki no kyojin the final season", k("Shingeki no Kyojin: The Final Season"))
	}
}
