package com.munoon.heartbeatlive.server.utils

import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.subscription.JwtUserSubscription
import com.munoon.heartbeatlive.server.user.UserRole
import org.springframework.graphql.test.tester.HttpGraphQlTester
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
}