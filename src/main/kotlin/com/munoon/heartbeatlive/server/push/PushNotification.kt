package com.munoon.heartbeatlive.server.push

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("pushNotification")
data class PushNotification(
    @Id
    val id: String? = null,

    val userId: String,

    val created: Instant = Instant.now(),

    val data: Data
) {
    sealed interface Data {
        data class NewSubscriberData(val subscriptionId: String) : Data
    }
}