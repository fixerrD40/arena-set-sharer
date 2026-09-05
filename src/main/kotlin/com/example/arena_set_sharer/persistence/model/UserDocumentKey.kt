package com.example.arena_set_sharer.persistence.model

import java.io.Serializable

data class UserDocumentKey(
    val userId: Int = 0,
    val type: String = "",
    val id: String = ""
) : Serializable
