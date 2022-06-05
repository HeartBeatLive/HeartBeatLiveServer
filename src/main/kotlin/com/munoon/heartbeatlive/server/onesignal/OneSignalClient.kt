package com.munoon.heartbeatlive.server.onesignal

import com.munoon.heartbeatlive.server.config.properties.OneSignalProperties
import com.munoon.heartbeatlive.server.onesignal.model.OneSignalSendNotification
import kotlinx.coroutines.reactor.mono
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
            .awaitBodyOrNull<Map<String, Any?>>()
            ?: throw OneSignalApiException("Received empty body when send notification")

        if (response["errors"] != null && response["recipients"] != 0) {
            throw OneSignalApiException("Receive error from OneSignal when trying to send push notification: $response")
        }
    }
}