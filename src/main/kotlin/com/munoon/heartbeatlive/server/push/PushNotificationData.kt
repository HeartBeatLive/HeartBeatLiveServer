package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.push.model.PushNotificationPriority

sealed interface PushNotificationData {
    val message: PushNotificationMessage
    val userIds: Set<String>
    val notification: PushNotification.Data?
    val priority: PushNotificationPriority
        get() = PushNotificationPriority.MEDIUM
}

sealed interface SingleUserPushNotificationData : PushNotificationData {
    val userId: String

    override val userIds: Set<String>
        get() = setOf(userId)
}

data class NewSubscriptionPushNotificationData(
    val subscriptionId: String,
    override val userId: String,
    val subscriberUserId: String,
    val subscriberDisplayName: String?
) : SingleUserPushNotificationData {
    override val message = NewSubscriptionPushNotificationMessage(subscriberDisplayName)
    override val notification = PushNotification.Data.NewSubscriberData(subscriptionId, subscriberUserId)
}

data class BanPushNotificationData(
    override val userId: String,
    val bannedByUserId: String,
    val bannedByUserDisplayName: String?
) : SingleUserPushNotificationData {
    override val message = BanPushNotificationMessage(bannedByUserDisplayName)
    override val notification = PushNotification.Data.BanData(bannedByUserId)
}

data class HighHeartRatePushNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String,
    val heartRateOwnerUserDisplayName: String?,
    override val userIds: Set<String>
) : PushNotificationData {
    override val message = HighHeartRatePushNotificationMessage(heartRate, heartRateOwnerUserDisplayName)
    override val notification = PushNotification.Data.HighHeartRateData(heartRateOwnerUserId, heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class LowHeartPushRateNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String,
    val heartRateOwnerUserDisplayName: String?,
    override val userIds: Set<String>
) : PushNotificationData {
    override val message = LowHeartRatePushNotificationMessage(heartRate, heartRateOwnerUserDisplayName)
    override val notification = PushNotification.Data.LowHeartRateData(heartRateOwnerUserId, heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class HighOwnHeartRatePushNotificationData(
    val heartRate: Float,
    override val userId: String,
) : SingleUserPushNotificationData {
    override val message = HighOwnHeartRatePushNotificationMessage(heartRate)
    override val notification = PushNotification.Data.HighOwnHeartRateData(heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class LowOwnHeartRatePushNotificationData(
    val heartRate: Float,
    override val userId: String,
) : SingleUserPushNotificationData {
    override val message = LowOwnHeartRatePushNotificationMessage(heartRate)
    override val notification = PushNotification.Data.LowOwnHeartRateData(heartRate)
    override val priority = PushNotificationPriority.HIGH
}

data class HeartRateMatchPushNotificationData(
    val heartRate: Float,
    override val userId: String,
    val matchWithUserId: String,
    val matchWithUserDisplayName: String?
) : SingleUserPushNotificationData {
    override val message = HeartRateMatchPushNotificationMessage(heartRate, matchWithUserDisplayName)
    override val notification = PushNotification.Data.HeartRateMatchData(heartRate, matchWithUserId)
}