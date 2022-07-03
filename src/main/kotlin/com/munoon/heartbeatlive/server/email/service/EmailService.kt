package com.munoon.heartbeatlive.server.email.service

import com.munoon.heartbeatlive.server.email.EmailMessage
import org.springframework.stereotype.Service

@Service
class EmailService {
    suspend fun send(message: EmailMessage) {
        println("Sending email: $message")
    }
}