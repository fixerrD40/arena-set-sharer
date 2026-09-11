package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.persistence.SetCommunityRepository
import com.example.arena_set_sharer.persistence.UserDocumentRepository
import com.example.arena_set_sharer.persistence.model.SetCommunityEntity
import com.example.arena_set_sharer.service.community.CommunityDocument
import com.example.arena_set_sharer.service.community.CommunityMath
import com.example.arena_set_sharer.service.community.FinalDeck
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Component
class SetCommunityService(
    private val documents: UserDocumentRepository,
    private val artifacts: SetCommunityRepository,
    private val objectMapper: ObjectMapper
) {
    private val dirty = ConcurrentHashMap.newKeySet<String>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "set-community").apply { isDaemon = true }
    }
    private val scheduleLock = Any()
    private var pending: ScheduledFuture<*>? = null

    fun markDirty(setId: String?) {
        val id = setId?.trim().orEmpty()
        if (id.isEmpty()) return
        dirty.add(id)
        synchronized(scheduleLock) {
            pending?.cancel(false)
            pending = scheduler.schedule({ flushDirty() }, DEBOUNCE_SECONDS, TimeUnit.SECONDS)
        }
    }

    fun read(setId: String): CommunityDocument {
        val existing = artifacts.findById(setId).orElse(null)
        if (existing != null) {
            return objectMapper.treeToValue(existing.body, CommunityDocument::class.java)
        }
        return CommunityMath.build(setId, emptyList(), Instant.EPOCH)
    }

    fun rebuild(setId: String) {
        val finals = documents.findByTypeAndDeletedFalse(DocumentService.TYPE_DECK)
            .filter { row ->
                row.body.path("setId").asText() == setId &&
                    row.body.path("status").asText() == STATUS_FINAL
            }
            .mapNotNull { row ->
                val cardsNode = row.body.get("cards") ?: return@mapNotNull null
                if (!cardsNode.isObject) return@mapNotNull null
                val cards = linkedMapOf<String, Int>()
                cardsNode.fields().forEachRemaining { (cardId, qty) ->
                    if (cardId.isNotBlank()) {
                        cards[cardId] = qty.asInt(0)
                    }
                }
                FinalDeck(cards)
            }

        val generatedAt = Instant.now()
        val document = CommunityMath.build(setId, finals, generatedAt)
        artifacts.save(
            SetCommunityEntity(
                setId = setId,
                generatedAt = generatedAt,
                body = objectMapper.valueToTree(document)
            )
        )
    }

    private fun flushDirty() {
        val ids = dirty.toSet()
        dirty.removeAll(ids)
        for (id in ids) {
            try {
                rebuild(id)
            } catch (err: Exception) {
                System.err.println("[SetCommunityService] Rebuild failed for $id: ${err.message}")
            }
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduler.shutdownNow()
    }

    companion object {
        private const val STATUS_FINAL = "final"
        private const val DEBOUNCE_SECONDS = 3L
    }
}
