package com.example.arena_set_sharer.persistence.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "user_documents")
@IdClass(UserDocumentKey::class)
data class UserDocumentEntity(
    @Id
    @Column(name = "user_id", nullable = false)
    val userId: Int,
    @Id
    @Column(nullable = false, length = 16)
    val type: String,
    @Id
    @Column(nullable = false, length = 64)
    val id: String,
    @Column(nullable = false)
    var deleted: Boolean = false,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var body: JsonNode
)
