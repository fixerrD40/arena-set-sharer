package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.api.model.OutboxRow
import com.example.arena_set_sharer.persistence.UserDocumentRepository
import com.example.arena_set_sharer.persistence.model.UserDocumentEntity
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DocumentService(
    private val documents: UserDocumentRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun ingest(userId: Int, rows: List<OutboxRow>) {
        rows.forEach { apply(userId, it) }
    }

    fun snapshot(userId: Int, type: String, contextId: String?): List<JsonNode> {
        val live = documents.findByUserIdAndTypeAndDeletedFalse(userId, type)
        if (contextId.isNullOrBlank() || contextId == ALL_CONTEXT) {
            return live.map { it.body }
        }
        return when (type) {
            TYPE_DECK -> live.filter { it.body.path("setId").asText() == contextId }.map { it.body }
            else -> live.filter { it.id == contextId }.map { it.body }
        }
    }

    private fun apply(userId: Int, row: OutboxRow) {
        val type = row.entityType.trim()
        require(type == TYPE_SET || type == TYPE_DECK) { "Unknown entityType: ${row.entityType}" }

        val action = row.action.trim()
        require(action == CREATE || action == UPDATE || action == DELETE) { "Unknown action: ${row.action}" }

        val documentId = row.entityId.trim().ifBlank {
            row.payload?.path("id")?.asText()?.trim().orEmpty()
        }
        require(documentId.isNotBlank()) { "Missing document id" }

        if (action != DELETE && type == TYPE_DECK) {
            val cards = row.payload?.get("cards")
            require(cards != null && cards.isObject) { "Deck upsert requires payload.cards" }
        }

        val incomingAt = incomingTimestamp(row)
        val existing = documents.findByUserIdAndTypeAndId(userId, type, documentId)
        if (existing != null && existing.updatedAt.isAfter(incomingAt)) {
            return
        }

        if (action == DELETE) {
            val body = existing?.body ?: objectMapper.createObjectNode()
            documents.save(
                UserDocumentEntity(
                    userId = userId,
                    type = type,
                    id = documentId,
                    deleted = true,
                    updatedAt = incomingAt,
                    body = body
                )
            )
            return
        }

        val payload = row.payload
        require(payload != null && payload.isObject) { "Upsert requires a JSON object payload" }

        documents.save(
            UserDocumentEntity(
                userId = userId,
                type = type,
                id = documentId,
                deleted = false,
                updatedAt = incomingAt,
                body = payload
            )
        )
    }

    private fun incomingTimestamp(row: OutboxRow): Instant {
        val raw = row.payload?.get("updatedAt")?.asText()?.takeIf { it.isNotBlank() }
            ?: row.createdAt?.takeIf { it.isNotBlank() }
        return raw?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Instant.now()
    }

    companion object {
        const val TYPE_SET = "set"
        const val TYPE_DECK = "deck"
        const val ALL_CONTEXT = "all"

        private const val CREATE = "CREATE"
        private const val UPDATE = "UPDATE"
        private const val DELETE = "DELETE"

        fun typeForSegment(segment: String): String? = when (segment) {
            "sets" -> TYPE_SET
            "decks" -> TYPE_DECK
            else -> null
        }
    }
}
