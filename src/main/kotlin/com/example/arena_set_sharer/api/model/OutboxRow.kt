package com.example.arena_set_sharer.api.model

import com.fasterxml.jackson.databind.JsonNode

data class OutboxRow(
    val entityType: String = "",
    val entityId: String = "",
    val action: String = "",
    val payload: JsonNode? = null,
    val createdAt: String? = null
)
