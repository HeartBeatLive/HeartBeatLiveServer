package com.munoon.heartbeatlive.server.config.websocket

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.heartrate.service.HeartRateService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.client.WebSocketGraphQlClient
import org.springframework.graphql.client.WebSocketGraphQlClientInterceptor
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class WebSocketAuthenticationInterceptorTest : AbstractTest() {
    @LocalServerPort
    private var webServerPort: Int = 8080

    @Autowired
    private lateinit var graphqlProperties: GraphQlProperties

    @MockkBean
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    @MockkBean
    private lateinit var heartRateService: HeartRateService

    @Test
    fun testAuthenticationUsingConnectionPayload() {
        every { heartRateService.subscribeToHeartRates("user1") } returns Flux.create {  }
        every { jwtDecoder.decode("CUSTOM_JWT_TOKEN") } returns Mono.just(Jwt
            .withTokenValue("CUSTOM_JWT_TOKEN")
            .header("alg", "RS256")
            .header("kid", "c673d3c947aeb18b64e58e3ede3255fbf575285b")
            .header("typ", "JWT")
            .subject("user1")
            .claim("name", "Nikita")
            .claim("roles", emptySet<String>())
            .claim("iss", "https://securetoken.google.com/heartbeat-c8956")
            .claim("auth", "heartbeat-c8956")
            .claim("user_id", "user1")
            .claim("email", "munoongg@gmail.com")
            .claim("email_verified", false)
            .claim("firebase", mapOf(
                "identities" to mapOf( "email" to setOf("munoongg@gmail.com") ),
                "sign_in_provider" to "password"
            ))
            .claim("auth_time", Instant.ofEpochSecond(1650908569))
            .claim("iat", Instant.ofEpochSecond(1650908569))
            .claim("exp", Instant.ofEpochSecond(1650912169))
            .build())
        val client = ReactorNettyWebSocketClient()

        val webSocketGraphQLClient = WebSocketGraphQlClient
            .builder("http://localhost:$webServerPort${graphqlProperties.websocket.path}", client)
            .build()
            .mutate()
            .interceptor(object : WebSocketGraphQlClientInterceptor {
                override fun connectionInitPayload(): Mono<Any> = Mono.just(mapOf(
                    "token" to "Bearer CUSTOM_JWT_TOKEN"
                ))
            })
            .build()

        val subscription = webSocketGraphQLClient.document("""
            subscription {
                subscribeToHeartRates {
                    subscriptionId, heartRate, ownHeartRate
                }
            }
        """.trimIndent()).executeSubscription()

        StepVerifier.create(subscription)
            .thenAwait(Duration.ofSeconds(5))
            .thenCancel()
            .verify(Duration.ofSeconds(10))

        verify(exactly = 1) { jwtDecoder.decode("CUSTOM_JWT_TOKEN") }
        verify(exactly = 1) { heartRateService.subscribeToHeartRates("user1") }
    }
}