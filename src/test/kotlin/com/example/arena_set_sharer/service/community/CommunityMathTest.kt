package com.example.arena_set_sharer.service.community

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CommunityMathTest {
    @Test
    fun emptyCorpus() {
        val doc = CommunityMath.build("set", emptyList(), Instant.EPOCH)
        assertEquals(0, doc.deckCount)
        assertTrue(doc.copies.isEmpty())
        assertTrue(doc.pairs.isEmpty())
    }

    @Test
    fun pairsAndCopyBuckets() {
        val decks = listOf(
            FinalDeck(mapOf("a" to 4, "b" to 1)),
            FinalDeck(mapOf("a" to 1, "b" to 1)),
            FinalDeck(mapOf("c" to 2))
        )
        val doc = CommunityMath.build("set", decks, Instant.parse("2026-09-10T00:00:00Z"))
        assertEquals(3, doc.deckCount)
        assertEquals(1, doc.copies.getValue("a").one)
        assertEquals(1, doc.copies.getValue("a").fourPlus)
        val ab = doc.pairs.first { it.a == "a" && it.b == "b" }
        assertEquals(2, ab.together)
        assertEquals(2.0 / 3.0, ab.support, 1e-9)
        assertEquals(1.5, ab.lift, 1e-9)
    }
}
