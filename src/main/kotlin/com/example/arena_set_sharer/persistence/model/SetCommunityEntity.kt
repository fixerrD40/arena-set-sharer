package com.example.arena_set_sharer.persistence.model

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "set_community")
data class SetCommunityEntity(
    @Id
    @Column(name = "set_id", nullable = false, length = 64)
    val setId: String,
    @Column(name = "generated_at", nullable = false)
    var generatedAt: Instant,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var body: JsonNode
)
