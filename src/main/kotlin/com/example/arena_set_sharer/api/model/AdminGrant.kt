package com.example.arena_set_sharer.api.model

data class AdminGrant(
    val email: String,
    val admin: Boolean = true
)
