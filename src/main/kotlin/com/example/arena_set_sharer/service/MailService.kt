package com.example.arena_set_sharer.service

interface MailService {

    fun sendEmail(toAddress: String, subject: String, body: String)
}