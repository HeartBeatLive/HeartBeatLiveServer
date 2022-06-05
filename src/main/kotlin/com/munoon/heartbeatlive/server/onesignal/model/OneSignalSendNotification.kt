package com.munoon.heartbeatlive.server.onesignal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OneSignalSendNotification(
    @SerialName("app_id") val appId: String = "",
    @SerialName("contents") val contents: Map<String, String>?,
    @SerialName("headings") val headings: Map<String, String>?,
    @SerialName("channel_for_external_user_ids") val channelForExternalUserIds: ChannelForExternalUserIds?,
    @SerialName("include_external_user_ids") val includeExternalUserIds: Set<String>?,
    @SerialName("data") val data: Map<String, JsonElement>?
) {
    @Serializable
    enum class ChannelForExternalUserIds {
        @SerialName("push") PUSH
    }
}