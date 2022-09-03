package com.munoon.heartbeatlive.server.push.model

import java.time.Instant

data class GraphqlPushNotification(
    val id: String,
    val created: Instant,
    val data: GraphqlPushNotificationData?
)

data class GraphqlPushNotificationInfo(
    val title: String,
    val content: String
)