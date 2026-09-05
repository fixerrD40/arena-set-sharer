package com.example.arena_set_sharer.api

import com.example.arena_set_sharer.api.model.OutboxRow
import com.example.arena_set_sharer.security.JwtRequestFilter
import com.example.arena_set_sharer.service.DocumentService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Document Rest Controller", description = "User document ingest and snapshot.")
@RequestMapping("/api")
class DocumentRestController(
    private val documents: DocumentService,
    private val objectMapper: ObjectMapper
) {

    @Operation(summary = "Ingest outbox NDJSON into user documents.")
    @PostMapping(
        "/outbox/bulk-sync",
        consumes = [MediaType.APPLICATION_NDJSON_VALUE, "application/x-ndjson"]
    )
    fun bulkSync(request: HttpServletRequest): ResponseEntity<Void> {
        val userId = JwtRequestFilter.authenticatedUser().id
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val rows = try {
            request.inputStream.bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { objectMapper.readValue(it, OutboxRow::class.java) }
                    .toList()
            }
        } catch (e: Exception) {
            return ResponseEntity.badRequest().build()
        }

        return try {
            documents.ingest(userId, rows)
            ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @Operation(summary = "Snapshot live documents for hydrate after login.")
    @GetMapping("/{segment}")
    fun snapshot(
        @PathVariable segment: String,
        @RequestParam(required = false) contextId: String?
    ): ResponseEntity<List<JsonNode>> {
        val type = DocumentService.typeForSegment(segment)
            ?: return ResponseEntity.notFound().build()
        val userId = JwtRequestFilter.authenticatedUser().id
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        return ResponseEntity.ok(documents.snapshot(userId, type, contextId))
    }
}
