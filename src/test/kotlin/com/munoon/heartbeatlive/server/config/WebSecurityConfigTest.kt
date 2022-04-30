package com.munoon.heartbeatlive.server.config

import com.google.firebase.ErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.munoon.heartbeatlive.server.AbstractTest
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.time.Instant

@SpringBootTest
internal class WebSecurityConfigTest : AbstractTest() {
    @MockkBean
    private lateinit var firebaseAuth: FirebaseAuth

    @Autowired
    private lateinit var jwtDecoder: ReactiveJwtDecoder

    @Test
    fun jwtDecoder() {
        val firebaseToken = mockk<FirebaseToken>()
        every { firebaseAuth.verifyIdToken(JWT_ID_TOKEN_VALUE) }.returns(firebaseToken)
        every { firebaseToken.uid }.returns(USER_ID)
        every { firebaseToken.claims }.returns(mapOf(
            "name" to "Nikita",
            "roles" to emptySet<String>(),
            "iss" to "https://securetoken.google.com/heartbeat-c8956",
            "auth" to "heartbeat-c8956",
            "user_id" to USER_ID,
            "email" to "munoongg@gmail.com",
            "email_verified" to false,
            "firebase" to mapOf(
                "identities" to mapOf( "email" to setOf("munoongg@gmail.com") ),
                "sign_in_provider" to "password"
            ),
            "auth_time" to 1650908569L,
            "iat" to 1650908569L,
            "exp" to 1650912169L
        ))

        val jwt = jwtDecoder.decode(JWT_ID_TOKEN_VALUE).block()

        assertThat(jwt).usingRecursiveComparison().isEqualTo(JWT_ID_TOKEN)
    }

    @Test
    fun `jwtDecoder - invalid token`() {
        every { firebaseAuth.verifyIdToken(any()) }
            .throws(FirebaseAuthException(ErrorCode.UNAUTHENTICATED, "Token invalid!", null, null, null))

        assertThatThrownBy { jwtDecoder.decode("invalid_jwt_token").block() }
            .isExactlyInstanceOf(JwtValidationException::class.java)
    }

    private companion object {
        const val JWT_ID_TOKEN_VALUE = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImM2NzNkM2M5NDdhZWIxOGI2NGU1OGUzZWRlMzI1NWZiZjU3NTI4NWIiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTmlraXRhIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczovL3NlY3VyZXRva2VuLmdvb2dsZS5jb20vaGVhcnRiZWF0LWM4OTU2IiwiYXVkIjoiaGVhcnRiZWF0LWM4OTU2IiwiYXV0aF90aW1lIjoxNjUwOTA4NTY5LCJ1c2VyX2lkIjoiRldZNGR1bVpGdU9LczRhVDNkTDNSVnNtVmtyMSIsInN1YiI6IkZXWTRkdW1aRnVPS3M0YVQzZEwzUlZzbVZrcjEiLCJpYXQiOjE2NTA5MDg1NjksImV4cCI6MTY1MDkxMjE2OSwiZW1haWwiOiJtdW5vb25nZ0BnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZW1haWwiOlsibXVub29uZ2dAZ21haWwuY29tIl19LCJzaWduX2luX3Byb3ZpZGVyIjoicGFzc3dvcmQifX0.OGj2ZOt_zSfr5AGBRus9QG1UwtswVNyaTLGhR8rw5ef6QjVCn8e7QZbR5fCpdGoXp3jsfwLWtRrQGNcikzeH-YItLnfqswnWLv40Tc5dIbdXWEmbvdIQyhcyXjpj0QSHYSTtDaX-SlQx79Hr6j6YdBeFT58ZU6tSgPm6c2h3Z-fRbmppVjYzdWO8p7Cr2IUNM-Zo2dm-OQi3CTQurNbWdBcyAlFo_jObqLCYTt0Gvklz2NlRoRh24fzML9bFlK7FGqkLdPKFiNP7FRj5Qs7xN9C7tCQJVmDP1bKW6Oh2eVuTnMShMcLTUN1CYz1sh1ZlwuKZcH4VNlJGnt7PVNI5og"
        val USER_ID = "FWY4dumZFuOKs4aT3dL3RVsmVkr1"
        val JWT_ID_TOKEN = Jwt.withTokenValue(JWT_ID_TOKEN_VALUE)
            .header("alg", "RS256")
            .header("kid", "c673d3c947aeb18b64e58e3ede3255fbf575285b")
            .header("typ", "JWT")
            .subject(USER_ID)
            .claim("name", "Nikita")
            .claim("roles", emptySet<String>())
            .claim("iss", "https://securetoken.google.com/heartbeat-c8956")
            .claim("auth", "heartbeat-c8956")
            .claim("user_id", USER_ID)
            .claim("email", "munoongg@gmail.com")
            .claim("email_verified", false)
            .claim("firebase", mapOf(
                "identities" to mapOf( "email" to setOf("munoongg@gmail.com") ),
                "sign_in_provider" to "password"
            ))
            .claim("auth_time", Instant.ofEpochSecond(1650908569))
            .claim("iat", Instant.ofEpochSecond(1650908569))
            .claim("exp", Instant.ofEpochSecond(1650912169))
            .build()
    }
}