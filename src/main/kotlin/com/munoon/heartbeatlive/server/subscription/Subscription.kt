package com.munoon.heartbeatlive.server.subscription

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("subscription")
data class Subscription(
    @Id
    val id: String? = null,

    val userId: String,

    val subscriberUserId: String,

    val created: Instant = Instant.now()
)