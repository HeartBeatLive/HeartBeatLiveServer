package com.munoon.heartbeatlive.server.utils

import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.subscription.account.JwtUserSubscription
import com.munoon.heartbeatlive.server.user.UserRole
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.WebSocketGraphQlTester
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication
import java.time.Duration
import java.time.Instant

object AuthTestUtils {
    fun HttpGraphQlTester.withUser(
        id: String = "1",
        email: String = "email@exmaple.com",
        emailVerified: Boolean = false,
        roles: Set<UserRole> = emptySet(),
        subscription: JwtUserSubscription = JwtUserSubscription.DEFAULT
    ): HttpGraphQlTester = mutate()
        .webTestClient {
            val jwt = Jwt.withTokenValue("mocked_token")
                .header("alg", "NONE")
                .subject(id)
                .claim("uid", id)
                .claim(CustomJwtAuthenticationToken.ROLES_CLAIM, roles.map(UserRole::name))
                .claim(CustomJwtAuthenticationToken.EMAIL_CLAIM, email)
                .claim(CustomJwtAuthenticationToken.EMAIL_VERIFIED_CLAIM, emailVerified)
                .claim(CustomJwtAuthenticationToken.SUBSCRIPTION_PLAN_CLAIM, subscription.asClaimsMap())
                .claim(JwtClaimNames.IAT, Instant.now())
                .claim(JwtClaimNames.EXP, Instant.now() + Duration.ofDays(1))
                .build()

            val authentication = CustomJwtAuthenticationToken(jwt)
            it.apply(mockAuthentication(authentication))
        }
        .build()

    fun WebSocketGraphQlTester.withUser(
        id: String = "1",
        email: String = "email@exmaple.com",
        emailVerified: Boolean = false,
        roles: Set<UserRole> = emptySet(),
        subscription: JwtUserSubscription = JwtUserSubscription.DEFAULT
    ): WebSocketGraphQlTester = mutate()
        .headers {
            val jwtToken = PlainJWT(JWTClaimsSet.parse(mapOf(
                "sub" to id,
                "uid" to id,
                CustomJwtAuthenticationToken.ROLES_CLAIM to roles.map(UserRole::name),
                CustomJwtAuthenticationToken.EMAIL_CLAIM to email,
                CustomJwtAuthenticationToken.EMAIL_VERIFIED_CLAIM to emailVerified,
                CustomJwtAuthenticationToken.SUBSCRIPTION_PLAN_CLAIM to subscription.asClaimsMap(),
                JwtClaimNames.IAT to Instant.now().epochSecond,
                JwtClaimNames.EXP to (Instant.now() + Duration.ofDays(1)).epochSecond,
                "auth_time" to Instant.now().epochSecond
            ))).serialize()

            it.setBearerAuth(jwtToken)
        }
        .build()
}