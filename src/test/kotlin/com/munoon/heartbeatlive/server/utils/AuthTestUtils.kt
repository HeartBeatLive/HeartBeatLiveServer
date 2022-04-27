package com.munoon.heartbeatlive.server.utils

import com.munoon.heartbeatlive.server.auth.CustomJwtAuthenticationToken
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
        roles: Set<UserRole> = emptySet()
    ): HttpGraphQlTester = mutate()
        .webTestClient {
            val jwt = Jwt.withTokenValue("mocked_token")
                .header("alg", "NONE")
                .subject(id)
                .claim("uid", id)
                .claim(CustomJwtAuthenticationToken.ROLES_CLAIM, roles.map(UserRole::name))
                .claim(JwtClaimNames.IAT, Instant.now())
                .claim(JwtClaimNames.EXP, Instant.now() + Duration.ofDays(1))
                .build()

            val authentication = CustomJwtAuthenticationToken(jwt)
            it.apply(mockAuthentication(authentication))
        }
        .build()
}