package com.munoon.heartbeatlive.server.push

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

sealed interface PushNotificationData {
    val title: Message
    val content: Message
    val metadata: Map<String, JsonElement>
    val userId: String
    val notification: PushNotification.Data?

    data class Message(val code: String, val arguments: List<Any?>) {
        constructor(code: String, vararg arguments: Any?) : this(code, arguments.toList())
    }
}

data class NewSubscriptionPushNotificationData(
    val subscriptionId: String,
    override val userId: String,
    val subscriberDisplayName: String
) : PushNotificationData {
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
) : PushNotificationData {
    override val title = PushNotificationData.Message("push_notifications.ban.title")
    override val content = PushNotificationData.Message(
        "push_notifications.ban.content", bannedByUserDisplayName)
    override val metadata = mapOf("banned_by_user_id" to JsonPrimitive(bannedByUserId))
    override val notification = PushNotification.Data.BanData(bannedByUserId)
}