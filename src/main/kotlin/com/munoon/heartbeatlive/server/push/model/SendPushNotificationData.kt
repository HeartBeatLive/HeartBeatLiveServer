package com.munoon.heartbeatlive.server.push.model

import kotlinx.serialization.json.JsonElement
import java.util.*

data class SendPushNotificationData(
    val userId: String,
    val title: Map<Locale, String>,
    val content: Map<Locale, String>,
    val metadata: Map<String, JsonElement>
)