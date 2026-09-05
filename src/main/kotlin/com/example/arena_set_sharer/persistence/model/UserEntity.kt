package com.example.arena_set_sharer.persistence.model

import com.example.arena_set_sharer.api.model.User
import jakarta.persistence.*
import java.time.Instant


@Entity
@Table(name = "users")
data class UserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    val emailHash: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
    val admin: Boolean = false
) {
    fun toDomain(root: Boolean = false): User = User(id, emailHash, username, passwordHash, admin, root)
}