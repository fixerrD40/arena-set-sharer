package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.api.model.User
import com.example.arena_set_sharer.persistence.UserRepository
import com.example.arena_set_sharer.persistence.model.UserEntity
import com.example.arena_set_sharer.security.CryptoUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Instant

@Component
class UserService(
    private val dao: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val cryptoUtil: CryptoUtil,
    @Value("\${spring.mail.username:}") private val mailUsername: String,
    @Value("\${spring.mail.password:}") private val mailPassword: String
) {

    fun getUser(id: Int): User {
        return dao.findById(id).get().toDomain()
    }

    fun getUser(email: String): User? {
        val encodedEmail = cryptoUtil.hmacSha256(email)
        return dao.findByEmailHash(encodedEmail)?.toDomain()
    }

    fun getUserByUsername(username: String): User? {
        return dao.findByUsername(username)?.toDomain()
    }

    fun authenticate(email: String, password: String): User? {
        val user = getUser(email) ?: return null
        if (!passwordEncoder.matches(password, user.getPassword())) return null
        return user
    }

    fun registerUser(credentials: User): User {
        require(credentials.email != null)
        require(EMAIL_REGEX.matches(credentials.email)) { "Invalid email format." }
        require(!isMailUsername(credentials.email)) { "That email is reserved." }
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

        return dao.save(newUser).toDomain()
    }

    fun markEmailVerified(userId: Int): User? {
        val entity = dao.findById(userId).orElse(null) ?: return null
        if (entity.emailVerified) return entity.toDomain()
        return dao.save(entity.copy(emailVerified = true)).toDomain()
    }

    fun resetUserPassword(userId: Int, newPassword: String): User? {
        val userOpt = dao.findById(userId)
        if (userOpt.isEmpty) return null

        val userEntity = userOpt.get()
        val encodedPassword = passwordEncoder.encode(newPassword)

        val updatedUser = userEntity.copy(passwordHash = encodedPassword)
        return dao.save(updatedUser).toDomain()
    }

    fun setAdmin(email: String, admin: Boolean): User? {
        require(EMAIL_REGEX.matches(email)) { "Invalid email format." }
        require(!isMailUsername(email)) { "Cannot change admin on the mail mailbox identity." }
        val entity = dao.findByEmailHash(cryptoUtil.hmacSha256(email)) ?: return null
        return dao.save(entity.copy(admin = admin)).toDomain()
    }

    /**
     * Ops handshake for PUT /api/admins: assert APP_MAIL_PASSWORD only.
     * Not a Spring role and not a session.
     */
    fun matchesMailRootPassword(password: String): Boolean {
        if (mailPassword.isBlank()) return false
        val expected = mailPassword.toByteArray(Charsets.UTF_8)
        val given = password.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expected, given)
    }

    private fun isMailUsername(email: String): Boolean {
        if (mailUsername.isBlank()) return false
        return email.trim().equals(mailUsername.trim(), ignoreCase = true)
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
    }
}
