package com.munoon.heartbeatlive.server.push

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.*

@Document("pushNotification")
data class PushNotification(
    @Id
    val id: String? = UUID.randomUUID().toString(),

    val userId: String,

    val created: Instant = Instant.now(),

    val data: Data
) {
    sealed interface Data {
        data class NewSubscriberData(val subscriptionId: String) : Data
        data class BanData(val bannedByUserId: String) : Data
        data class HighHeartRateData(val heartRateOwnerUserId: String, val heartRate: Float) : Data
        data class LowHeartRateData(val heartRateOwnerUserId: String, val heartRate: Float) : Data
        data class HighOwnHeartRateData(val heartRate: Float) : Data
        data class LowOwnHeartRateData(val heartRate: Float) : Data
        data class HeartRateMatchData(val heartRate: Float, val matchWithUserId: String) : Data
    }
}