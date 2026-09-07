package com.example.arena_set_sharer.service

import com.example.arena_set_sharer.api.model.User
import com.example.arena_set_sharer.persistence.EmailVerificationRepository
import com.example.arena_set_sharer.persistence.model.EmailVerificationEntity
import com.example.arena_set_sharer.security.CryptoUtil
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class EmailVerificationService(
    private val dao: EmailVerificationRepository,
    private val users: UserService,
    private val crypto: CryptoUtil,
    private val mailSender: MailService,
    @Value("\${app.base-url}") private val baseUrl: String
) {

    @Transactional
    fun sendForNewUser(user: User, email: String) {
        dao.deleteByAppUser(user.id!!)
        val rawToken = generateAndSaveToken(user)
        mailSender.sendEmail(
            toAddress = email,
            subject = "Verify your Arena Set Cracker email",
            body = verificationBody(rawToken)
        )
    }

    @Transactional
    fun resend(email: String) {
        val user = users.getUser(email) ?: return
        if (user.emailVerified) return
        if (!canResend(user)) return

        dao.deleteByAppUser(user.id!!)
        val rawToken = generateAndSaveToken(user)
        mailSender.sendEmail(
            toAddress = email,
            subject = "Verify your Arena Set Cracker email",
            body = verificationBody(rawToken)
        )
    }

    @Transactional
    fun confirm(token: String): User {
        val hash = crypto.hmacSha256(token)
        val row = dao.findValidByTokenHash(hash)
            ?: throw IllegalStateException("Invalid or expired verification token")

        row.used = true
        dao.save(row)

        return users.markEmailVerified(row.appUser)
            ?: throw IllegalStateException("User not found")
    }

    private fun canResend(user: User): Boolean {
        val recent = dao.findTopByAppUserOrderByCreatedAtDesc(user.id!!)
        return recent == null || Duration.between(recent.createdAt, Instant.now()) > Duration.ofHours(1)
    }

    private fun generateAndSaveToken(user: User): String {
        val token = UUID.randomUUID().toString()
        dao.save(
            EmailVerificationEntity(
                appUser = user.id!!,
                tokenHash = crypto.hmacSha256(token),
                expiresAt = Instant.now().plus(Duration.ofHours(24)),
                used = false
            )
        )
        return token
    }

    private fun verificationBody(rawToken: String): String {
        val link = "$baseUrl/verify-email?token=$rawToken"
        return "Confirm your email for Arena Set Cracker:\n\n$link\n\nIf you did not create this account, you can ignore this message."
    }
}
