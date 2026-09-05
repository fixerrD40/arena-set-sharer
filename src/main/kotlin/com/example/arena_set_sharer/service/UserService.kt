package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.api.model.User
import com.example.arena_set_sharer.persistence.UserRepository
import com.example.arena_set_sharer.persistence.model.UserEntity
import com.example.arena_set_sharer.security.CryptoUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserService(
    private val dao: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val cryptoUtil: CryptoUtil
) {

    fun getUser(id: Int): User {
        return dao.findById(id).get().toDomain()
    }

    fun getUser(email: String): User? {
        val encodedEmail = cryptoUtil.hmacSha256(email)

        return dao.findByEmailHash(encodedEmail)?.toDomain()
    }

    fun authenticate(email: String, password: String): User? {
        val user = getUser(email) ?: return null
        if (!passwordEncoder.matches(password, user.getPassword())) return null
        return user
    }

    fun registerUser(credentials: User): User {
        require(credentials.email != null)
        require(EMAIL_REGEX.matches(credentials.email)) { "Invalid email format." }
        val encodedEmail = cryptoUtil.hmacSha256(credentials.email)
        val encodedPassword = passwordEncoder.encode(credentials.password)
        val username = credentials.username.takeIf { it.isNotBlank() }
            ?: credentials.email.substringBefore('@').take(50)
        require(username.isNotBlank()) { "Username is required." }

        val newUser = UserEntity(
            emailHash = encodedEmail,
            username = username,
            passwordHash = encodedPassword,
            createdAt = Instant.now()
        )

        return dao.save(newUser).toDomain()
    }

    fun resetUserPassword(userId: Int, newPassword: String): User? {
        val userOpt = dao.findById(userId)
        if (userOpt.isEmpty) return null

        val userEntity = userOpt.get()
        val encodedPassword = passwordEncoder.encode(newPassword)

        val updatedUser = userEntity.copy(passwordHash = encodedPassword)
        return dao.save(updatedUser).toDomain()
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    }
}