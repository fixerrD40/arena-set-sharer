package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.api.model.User
import com.example.arena_set_sharer.persistence.UserRepository
import com.example.arena_set_sharer.persistence.model.UserEntity
import com.example.arena_set_sharer.security.CryptoUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class UserService(
    private val dao: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val cryptoUtil: CryptoUtil,
    @Value("\${spring.mail.username:}") private val mailUsername: String
) {

    fun getUser(id: Int): User {
        return toDomain(dao.findById(id).get())
    }

    fun getUser(email: String): User? {
        val encodedEmail = cryptoUtil.hmacSha256(email)
        return dao.findByEmailHash(encodedEmail)?.let { toDomain(it) }
    }

    fun getUserByUsername(username: String): User? {
        return dao.findByUsername(username)?.let { toDomain(it) }
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
            createdAt = Instant.now(),
            admin = false,
            emailVerified = false
        )

        return toDomain(dao.save(newUser))
    }

    fun markEmailVerified(userId: Int): User? {
        val entity = dao.findById(userId).orElse(null) ?: return null
        if (entity.emailVerified) return toDomain(entity)
        return toDomain(dao.save(entity.copy(emailVerified = true)))
    }

    fun resetUserPassword(userId: Int, newPassword: String): User? {
        val userOpt = dao.findById(userId)
        if (userOpt.isEmpty) return null

        val userEntity = userOpt.get()
        val encodedPassword = passwordEncoder.encode(newPassword)

        val updatedUser = userEntity.copy(passwordHash = encodedPassword)
        return toDomain(dao.save(updatedUser))
    }

    fun setAdmin(email: String, admin: Boolean): User? {
        require(EMAIL_REGEX.matches(email)) { "Invalid email format." }
        val entity = dao.findByEmailHash(cryptoUtil.hmacSha256(email)) ?: return null
        return toDomain(dao.save(entity.copy(admin = admin)))
    }

    // root is the mail mailbox (spring.mail.username HMAC), not a grant. admin is a column root can flip.
    private fun toDomain(entity: UserEntity): User = entity.toDomain(isRootHash(entity.emailHash))

    private fun isRootHash(emailHash: String): Boolean {
        if (mailUsername.isBlank()) return false
        return emailHash == cryptoUtil.hmacSha256(mailUsername)
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    }
}
