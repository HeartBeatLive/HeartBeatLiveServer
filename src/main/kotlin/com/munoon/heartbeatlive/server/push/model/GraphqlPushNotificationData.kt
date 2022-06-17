package com.munoon.heartbeatlive.server.push.model

sealed interface GraphqlPushNotificationData

data class GraphqlNewSubscriberPushNotificationData(
    val subscriptionId: String // for mapping only
) : GraphqlPushNotificationData

data class GraphqlBannedPushNotificationData(
    val bannedByUserId: String // for mapping only
) : GraphqlPushNotificationData

data class GraphqlHighHeartRatePushNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String // for mapping only
) : GraphqlPushNotificationData

data class GraphqlLowHeartRatePushNotificationData(
    val heartRate: Float,
    val heartRateOwnerUserId: String // for mapping only
) : GraphqlPushNotificationData

data class GraphqlHighOwnHeartRatePushNotificationData(
    val heartRate: Float
) : GraphqlPushNotificationData

data class GraphqlLowOwnHeartRatePushNotificationData(
    val heartRate: Float
) : GraphqlPushNotificationData

data class GraphqlHeartRateMatchPushNotificationData(
    val heartRate: Float,
    val matchWithUserId: String // for mapping only
) : GraphqlPushNotificationData