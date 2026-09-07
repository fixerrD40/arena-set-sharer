package com.example.arena_set_sharer.persistence

import com.example.arena_set_sharer.persistence.model.EmailVerificationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmailVerificationRepository : JpaRepository<EmailVerificationEntity, Int> {

    fun findTopByAppUserOrderByCreatedAtDesc(userId: Int): EmailVerificationEntity?

    @Query(
        "SELECT e FROM EmailVerificationEntity e WHERE e.tokenHash = :tokenHash AND e.used = false AND e.expiresAt > CURRENT_TIMESTAMP"
    )
    fun findValidByTokenHash(@Param("tokenHash") tokenHash: String): EmailVerificationEntity?

    fun deleteByAppUser(userId: Int): Long
}
