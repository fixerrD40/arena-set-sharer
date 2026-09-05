package com.example.arena_set_sharer.api.model

data class PasswordReset(
    val token: String,
    val newPassword: String
)