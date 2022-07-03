package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.heartrate.HeartRateUtils

sealed interface PushNotificationMessage {
    val title: Message
    val content: Message

    data class Message(val code: String, val arguments: List<Any?>) {
        constructor(code: String, vararg arguments: Any?) : this(code, arguments.toList())
    }
}

data class NewSubscriptionPushNotificationMessage(
    val subscriberDisplayName: String?
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.new_subscription.title")
    override val content = PushNotificationMessage.Message(
        "push_notifications.new_subscription.content", subscriberDisplayName ?: "User")
}

data class BanPushNotificationMessage(
    val bannedByUserDisplayName: String?
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.ban.title")
    override val content = PushNotificationMessage.Message(
        "push_notifications.ban.content", bannedByUserDisplayName ?: "User")
}

data class HighHeartRatePushNotificationMessage(
    val heartRate: Float,
    val heartRateOwnerUserDisplayName: String?
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message(
        "push_notifications.high_heart_rate.title", heartRateOwnerUserDisplayName ?: "User")
    override val content = PushNotificationMessage.Message("push_notifications.high_heart_rate.content",
        heartRateOwnerUserDisplayName ?: "User", HeartRateUtils.mapHeartRateToInteger(heartRate))
}

data class LowHeartRatePushNotificationMessage(
    val heartRate: Float,
    val heartRateOwnerUserDisplayName: String?
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message(
        "push_notifications.low_heart_rate.title", heartRateOwnerUserDisplayName ?: "User")
    override val content = PushNotificationMessage.Message("push_notifications.low_heart_rate.content",
        heartRateOwnerUserDisplayName ?: "User", HeartRateUtils.mapHeartRateToInteger(heartRate))
}

data class HighOwnHeartRatePushNotificationMessage(
    val heartRate: Float
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.high_own_heart_rate.title")
    override val content = PushNotificationMessage.Message(
        "push_notifications.high_own_heart_rate.content", HeartRateUtils.mapHeartRateToInteger(heartRate))
}

data class LowOwnHeartRatePushNotificationMessage(
    val heartRate: Float
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.low_own_heart_rate.title")
    override val content = PushNotificationMessage.Message(
        "push_notifications.low_own_heart_rate.content", HeartRateUtils.mapHeartRateToInteger(heartRate))
}

data class HeartRateMatchPushNotificationMessage(
    val heartRate: Float,
    val matchWithUserDisplayName: String?
) : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.heart_rate_match.title")
    override val content = PushNotificationMessage.Message(
        "push_notifications.heart_rate_match.content",
        HeartRateUtils.mapHeartRateToInteger(heartRate), matchWithUserDisplayName ?: "User")
}

object FailedToRefundPushNotificationMessage : PushNotificationMessage {
    override val title = PushNotificationMessage.Message("push_notifications.failed_refund.title")
    override val content = PushNotificationMessage.Message("push_notifications.failed_refund.content")
}