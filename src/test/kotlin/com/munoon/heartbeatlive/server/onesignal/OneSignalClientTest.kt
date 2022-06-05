package com.munoon.heartbeatlive.server.onesignal

import com.munoon.heartbeatlive.server.config.properties.OneSignalProperties
import com.munoon.heartbeatlive.server.onesignal.model.OneSignalSendNotification
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.mock.http.client.reactive.MockClientHttpRequest
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

class OneSignalClientTest : FreeSpec({
    val emptyNotification = OneSignalSendNotification(
        "", null, null, null, null, null)

    val properties = OneSignalProperties().apply {
        appId = "one-signal-first-app"
        restApiKey = "super-secret-rest-api-key"
    }

    val oneSignalSendNotificationArbitrary = arbitrary {
        OneSignalSendNotification(
            appId = "",
            contents = Arb.map(Arb.stringPattern("en|ru|ua"), Arb.string(minSize = 2, maxSize = 100)).bind(),
            headings = Arb.map(Arb.stringPattern("en|ru|ua"), Arb.string(minSize = 2, maxSize = 100)).bind(),
            channelForExternalUserIds = Arb.enum<OneSignalSendNotification.ChannelForExternalUserIds>().bind(),
            includeExternalUserIds = Arb.set(Arb.uuid().map { it.toString() }, size = 1).bind(),
            data = Arb.map(
                Arb.string(),
                Arb.bind(listOf(Arb.string())) { JsonPrimitive(it.first()) },
                minSize = 1, maxSize = 10
            ).bind()
        )
    }

    "sendNotification" {
        checkAll(10, oneSignalSendNotificationArbitrary) { notification ->
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()
            val client = OneSignalClient(properties, webClient)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{}")
                    .build()
            }

            shouldNotThrowAny { client.sendNotification(notification) }

            verify(exactly = 1) { webClientExchangeFunction.exchange(match {
                it.headers().accept.first() shouldBe MediaType.APPLICATION_JSON
                it.headers().contentType shouldBe MediaType.APPLICATION_JSON
                it.headers()[HttpHeaders.AUTHORIZATION]?.first() shouldBe "Basic super-secret-rest-api-key"
                it.url().toString() shouldBe "https://onesignal.com/api/v1/notifications"

                val request = MockClientHttpRequest(HttpMethod.POST, "https://onesignal.com/api/v1/notifications")
                it.body().insert(request, object : BodyInserter.Context {
                    val hints = mutableMapOf<String, Any>()
                    override fun messageWriters(): MutableList<HttpMessageWriter<*>> = mutableListOf(
                        EncoderHttpMessageWriter(KotlinSerializationJsonEncoder())
                    )
                    override fun serverRequest() = Optional.empty<ServerHttpRequest>()
                    override fun hints(): MutableMap<String, Any> = hints
                }).block()
                Json.decodeFromString<OneSignalSendNotification>(request.bodyAsString.block()!!) shouldBe
                        notification.copy(appId = "one-signal-first-app")

                true
            }) }
        }
    }

    "sendNotification - errors" - {
        "response status" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()
            val client = OneSignalClient(properties, webClient)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{}")
                    .build()
            }

            shouldThrow<OneSignalApiException> {
                client.sendNotification(emptyNotification)
            }
        }

        "empty response" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()
            val client = OneSignalClient(properties, webClient)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .build()
            }

            shouldThrow<OneSignalApiException> {
                client.sendNotification(emptyNotification)
            }
        }

        "response body contains errors" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()
            val client = OneSignalClient(properties, webClient)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""{"recipients":1,"errors":["Example error"]}""")
                    .build()
            }

            shouldThrow<OneSignalApiException> {
                client.sendNotification(emptyNotification)
            }
        }

        "response body contain 'no subscribers' error" {
            val webClientExchangeFunction = mockk<ExchangeFunction>()
            val webClient = WebClient.builder()
                .exchangeFunction(webClientExchangeFunction)
                .build()
            val client = OneSignalClient(properties, webClient)

            every { webClientExchangeFunction.exchange(any()) } returns mono {
                ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("""{"recipients":0,"errors":["No subscribers"]}""")
                    .build()
            }

            shouldNotThrowAny {
                client.sendNotification(emptyNotification)
            }
        }
    }
})