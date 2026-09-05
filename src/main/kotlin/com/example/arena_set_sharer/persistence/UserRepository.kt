package com.example.arena_set_sharer.persistence

import com.example.arena_set_sharer.persistence.model.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByUsername(username: String): UserEntity?

    fun findByEmailHash(emailHash: String): UserEntity?
}