package com.example.arena_set_sharer.service.community

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CopyBuckets(
    @get:JsonProperty("1") val one: Int = 0,
    @get:JsonProperty("2") val two: Int = 0,
    @get:JsonProperty("3") val three: Int = 0,
    @get:JsonProperty("4+") val fourPlus: Int = 0
)

object CommunityMath {
    fun build(setId: String, decks: List<FinalDeck>, generatedAt: Instant): CommunityDocument {
        val deckCount = decks.size
        if (deckCount == 0) {
            return CommunityDocument(
                setId = setId,
                deckCount = 0,
                generatedAt = generatedAt.toString(),
                copies = emptyMap(),
                pairs = emptyList()
            )
        }

        val copies = linkedMapOf<String, IntArray>()
        val presence = decks.map { deck ->
            deck.cards.filterValues { it > 0 }.keys
        }

        for (deck in decks) {
            for ((cardId, qty) in deck.cards) {
                if (qty <= 0) continue
                val buckets = copies.getOrPut(cardId) { IntArray(4) }
                val slot = when {
                    qty <= 1 -> 0
                    qty == 2 -> 1
                    qty == 3 -> 2
                    else -> 3
                }
                buckets[slot] += 1
            }
        }

        val cardSupport = presence.flatten().groupingBy { it }.eachCount()
        val pairTogether = hashMapOf<String, Int>()
        for (ids in presence) {
            val ordered = ids.sorted()
            for (i in ordered.indices) {
                for (j in i + 1 until ordered.size) {
                    val key = "${ordered[i]}\u0000${ordered[j]}"
                    pairTogether[key] = (pairTogether[key] ?: 0) + 1
                }
            }
        }

        val n = deckCount.toDouble()
        val pairs = pairTogether.entries.map { (key, together) ->
            val parts = key.split('\u0000')
            val a = parts[0]
            val b = parts[1]
            val decksA = cardSupport[a] ?: 0
            val decksB = cardSupport[b] ?: 0
            val support = together / n
            val supportA = decksA / n
            val supportB = decksB / n
            val lift = if (supportA > 0 && supportB > 0) support / (supportA * supportB) else 0.0
            CommunityPair(
                a = a,
                b = b,
                support = support,
                confidenceAb = if (decksA > 0) together.toDouble() / decksA else 0.0,
                confidenceBa = if (decksB > 0) together.toDouble() / decksB else 0.0,
                lift = lift,
                together = together
            )
        }.sortedWith(compareByDescending<CommunityPair> { it.lift }.thenBy { it.a }.thenBy { it.b })

        return CommunityDocument(
            setId = setId,
            deckCount = deckCount,
            generatedAt = generatedAt.toString(),
            copies = copies.entries
                .sortedBy { it.key }
                .associate { (id, buckets) ->
                    id to CopyBuckets(
                        one = buckets[0],
                        two = buckets[1],
                        three = buckets[2],
                        fourPlus = buckets[3]
                    )
                },
            pairs = pairs
        )
    }
}
