package com.munoon.heartbeatlive.server

import com.nimbusds.jwt.JWTParser
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.graphql.test.tester.WebSocketGraphQlTester
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractGraphqlSubscriptionTest : AbstractTest() {
    @LocalServerPort
    private var webServerPort: Int = 8080

    @Autowired
    private lateinit var graphqlProperties: GraphQlProperties

    @MockkBean
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    protected lateinit var graphqlTester: WebSocketGraphQlTester

    @BeforeEach
    fun setUpGraphqlTester() {
        val idToken = slot<String>()
        every { jwtDecoder.decode(capture(idToken)) } answers {
            val jwtToken = JWTParser.parse(idToken.captured)
            Mono.just(Jwt.withTokenValue(jwtToken.parsedString)
                .claims { it.putAll(jwtToken.jwtClaimsSet.toJSONObject()) }
                .headers { it.putAll(jwtToken.header.toJSONObject()) }
                .claim(JwtClaimNames.IAT, jwtToken.jwtClaimsSet.getDateClaim("iat").toInstant())
                .claim(JwtClaimNames.EXP, jwtToken.jwtClaimsSet.getDateClaim("exp").toInstant())
                .claim("auth_time", Instant.ofEpochSecond(jwtToken.jwtClaimsSet.getLongClaim("auth_time")))
                .build())
        }

        val client = ReactorNettyWebSocketClient()

        graphqlTester = WebSocketGraphQlTester
            .builder("http://localhost:$webServerPort${graphqlProperties.websocket.path}", client)
            .build()
    }
}