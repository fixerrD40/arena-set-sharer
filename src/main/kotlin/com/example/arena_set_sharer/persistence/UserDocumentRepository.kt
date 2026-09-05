package com.example.arena_set_sharer.persistence

import com.example.arena_set_sharer.persistence.model.UserDocumentEntity
import com.example.arena_set_sharer.persistence.model.UserDocumentKey
import org.springframework.data.jpa.repository.JpaRepository

interface UserDocumentRepository : JpaRepository<UserDocumentEntity, UserDocumentKey> {
    fun findByUserIdAndTypeAndDeletedFalse(userId: Int, type: String): List<UserDocumentEntity>

    fun findByUserIdAndTypeAndId(userId: Int, type: String, id: String): UserDocumentEntity?
}
