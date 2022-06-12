package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.heartrate.HeartRateUtils.mapHeartRateToInteger
import com.munoon.heartbeatlive.server.push.model.PushNotificationPriority
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

sealed interface PushNotificationData {
    val title: Message
    val content: Message
    val metadata: Map<String, JsonElement>
    val userIds: Set<String>
    val notification: PushNotification.Data?
    val priority: PushNotificationPriority
        get() = PushNotificationPriority.MEDIUM

    data class Message(val code: String, val arguments: List<Any?>) {
        constructor(code: String, vararg arguments: Any?) : this(code, arguments.toList())
    }
}

sealed interface SingleUserPushNotificationData : PushNotificationData {
    val userId: String

    override val userIds: Set<String>
        get() = setOf(userId)
}

data class NewSubscriptionPushNotificationData(
    val subscriptionId: String,
    override val userId: String,
    val subscriberDisplayName: String
) : SingleUserPushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.new_subscription.title")
    override val content = PushNotificationData.Message(
        "push_notifications.new_subscription.content", subscriberDisplayName)
    override val metadata = mapOf("subscription_id" to JsonPrimitive(subscriptionId))
    override val notification = PushNotification.Data.NewSubscriberData(subscriptionId)
}

data class BanPushNotificationData(
    override val userId: String,
    val bannedByUserId: String,
    val bannedByUserDisplayName: String
) : SingleUserPushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.ban.title")
    override val content = PushNotificationData.Message(
        "push_notifications.ban.content", bannedByUserDisplayName)
    override val metadata = mapOf("banned_by_user_id" to JsonPrimitive(bannedByUserId))
    override val notification = PushNotification.Data.BanData(bannedByUserId)
}

data class HighHeartRateNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String,
    val heartRateOwnerUserDisplayName: String,
    override val userIds: Set<String>
) : PushNotificationData {
    override val title = PushNotificationData.Message(
        "push_notifications.high_heart_rate.title", heartRateOwnerUserDisplayName)
    override val content = PushNotificationData.Message(
        "push_notifications.high_heart_rate.content",
        heartRateOwnerUserDisplayName, mapHeartRateToInteger(heartRate))
    override val metadata = mapOf(
        "heart_rate_owner_user_id" to JsonPrimitive(heartRateOwnerUserId), // TODO remove
        "heart_rate" to JsonPrimitive(heartRate)
    )
    override val notification = PushNotification.Data.HighHeartRateData(heartRateOwnerUserId, heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class LowHeartRateNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String,
    val heartRateOwnerUserDisplayName: String,
    override val userIds: Set<String>
) : PushNotificationData {
    override val title = PushNotificationData.Message(
        "push_notifications.low_heart_rate.title", heartRateOwnerUserDisplayName)
    override val content = PushNotificationData.Message(
        "push_notifications.low_heart_rate.content",
        heartRateOwnerUserDisplayName, mapHeartRateToInteger(heartRate))
    override val metadata = mapOf(
        "heart_rate_owner_user_id" to JsonPrimitive(heartRateOwnerUserId), // TODO remove
        "heart_rate" to JsonPrimitive(heartRate)
    )
    override val notification = PushNotification.Data.LowHeartRateData(heartRateOwnerUserId, heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class HighOwnHeartRateNotificationData(
    val heartRate: Float,
    override val userId: String,
) : SingleUserPushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.high_own_heart_rate.title")
    override val content = PushNotificationData.Message(
        "push_notifications.high_own_heart_rate.content", mapHeartRateToInteger(heartRate))
    override val metadata = mapOf("heart_rate" to JsonPrimitive(heartRate))
    override val notification = PushNotification.Data.HighOwnHeartRateData(heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class LowOwnHeartRateNotificationData(
    val heartRate: Float,
    override val userId: String,
) : SingleUserPushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.low_own_heart_rate.title")
    override val content = PushNotificationData.Message(
        "push_notifications.low_own_heart_rate.content", mapHeartRateToInteger(heartRate))
    override val metadata = mapOf("heart_rate" to JsonPrimitive(heartRate))
    override val notification = PushNotification.Data.LowOwnHeartRateData(heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class HeartRateMatchNotificationData(
    val heartRate: Float,
    override val userId: String,
    val matchWithUserId: String,
    val matchWithUserDisplayName: String,
    val subscriptionId: String
) : SingleUserPushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.heart_rate_match.title")
    override val content = PushNotificationData.Message(
        "push_notifications.heart_rate_match.content",
        mapHeartRateToInteger(heartRate), matchWithUserDisplayName)
    override val metadata = mapOf(
        "heart_rate" to JsonPrimitive(heartRate),
        "subscription_id" to JsonPrimitive(subscriptionId)
    )
    override val notification = PushNotification.Data.HeartRateMatchData(heartRate, matchWithUserId)
}