package com.munoon.heartbeatlive.server.onesignal

import com.munoon.heartbeatlive.server.config.properties.OneSignalProperties
import com.munoon.heartbeatlive.server.onesignal.model.OneSignalSendNotification
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Component
class OneSignalClient(
    private val oneSignalProperties: OneSignalProperties,
    private val webClient: WebClient
) {
    @Suppress("ThrowsCount")
    suspend fun sendNotification(notification: OneSignalSendNotification) {
        val body = notification.takeIf { it.appId.isNotEmpty() }
            ?: notification.copy(appId = oneSignalProperties.appId)

        val response = webClient.post()
            .uri("https://onesignal.com/api/v1/notifications")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "Basic ${oneSignalProperties.restApiKey}")
            .bodyValue(body)
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { mono {
                val errorBody = it.awaitBody<Map<String, Any?>>()
                throw OneSignalApiException("Received ${it.statusCode()} response status: $errorBody")
            } }
            .awaitBodyOrNull<SendNotificationResponse>()
            ?: throw OneSignalApiException("Received empty body when send notification")

        if (response.errors != null) {
            // ignore 'No receivers' error
            if (response.recipients == 0L && response.errors is JsonArray && response.errors.size == 1) {
                return
            }

            // ignore 'External user id Not Found' error
            if (response.errors is JsonObject
                    && response.errors.size == 1
                    && response.errors["invalid_external_user_ids"] != null) {
                return
            }

            throw OneSignalApiException("Receive error from OneSignal when trying to send push notification: $response")
        }
    }

    private companion object {
        @Serializable
        data class SendNotificationResponse(
            @SerialName("id") val id: String? = null,
            @SerialName("recipients") val recipients: Long? = null,
            @SerialName("external_id") val externalId: JsonElement? = null,
            @SerialName("errors") val errors: JsonElement? = null
        )
    }
}