package com.example.arena_set_sharer.api.model

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class User(
    val id: Int? = null,
    val email: String? = null,
    private val username: String = "",
    private val password: String = "",
    val admin: Boolean = false,
    val root: Boolean = false
) : UserDetails {
    override fun getAuthorities(): Set<GrantedAuthority> = buildSet {
        if (root) add(SimpleGrantedAuthority("ROLE_ROOT"))
        if (admin || root) add(SimpleGrantedAuthority("ROLE_ADMIN"))
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }
}