package com.example.arena_set_sharer.api.model

/** Target grant plus APP_MAIL_PASSWORD handshake (not a Spring role / session). */
data class AdminGrant(
    val email: String,
    val admin: Boolean = true,
    val rootPassword: String = ""
)
