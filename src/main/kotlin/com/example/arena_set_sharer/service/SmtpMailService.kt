package com.example.arena_set_sharer.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class SmtpMailService(
    private val mailSender: JavaMailSender,
    @Value("\${spring.mail.username}") private val fromAddress: String
) : MailService {

    override fun sendEmail(toAddress: String, subject: String, body: String) {
        val message = SimpleMailMessage()
        message.setFrom(fromAddress)
        message.setTo(toAddress)
        message.subject = subject
        message.text = body
        mailSender.send(message)
    }
}
