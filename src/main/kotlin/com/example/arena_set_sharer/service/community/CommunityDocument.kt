package com.example.arena_set_sharer.service.community

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CommunityDocument(
    val setId: String,
    val deckCount: Int,
    val generatedAt: String,
    val copies: Map<String, CopyBuckets>,
    val pairs: List<CommunityPair>
)

data class CommunityPair(
    val a: String,
    val b: String,
    val support: Double,
    val confidenceAb: Double,
    val confidenceBa: Double,
    val lift: Double,
    val together: Int
)

data class FinalDeck(val cards: Map<String, Int>)
