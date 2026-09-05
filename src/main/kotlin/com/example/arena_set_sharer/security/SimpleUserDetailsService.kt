package com.example.arena_set_sharer.security

import com.example.arena_set_sharer.service.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class SimpleUserDetailsService(private val users: UserService) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        return users.getUserByUsername(username)
            ?: throw UsernameNotFoundException("User not found")
    }
}
