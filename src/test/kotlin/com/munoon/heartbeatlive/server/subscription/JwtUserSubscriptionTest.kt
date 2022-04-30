package com.munoon.heartbeatlive.server.subscription

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class JwtUserSubscriptionTest {
    @Test
    fun asClaimsMap() {
        val expirationTime = Instant.now()
        val expectedClaims = mapOf("plan" to "PRO", "exp" to  expirationTime.epochSecond)

        val claims = JwtUserSubscription(UserSubscriptionPlan.PRO, expirationTime).asClaimsMap()
        assertThat(claims).isEqualTo(expectedClaims)
    }

    @Test
    fun createFromJwtClaimMap() {
        val expirationTimeEpochSeconds = Instant.now().epochSecond
        val expectedJwtUserSubscription = JwtUserSubscription(UserSubscriptionPlan.PRO, Instant.ofEpochSecond(expirationTimeEpochSeconds))

        val claims = mapOf("plan" to "PRO", "exp" to  expirationTimeEpochSeconds)
        assertThat(JwtUserSubscription.createFromJwtClaimMap(claims)).isEqualTo(expectedJwtUserSubscription)
    }

    @Test
    fun `createFromJwtClaimMap - no plan`() {
        val expected = JwtUserSubscription(UserSubscriptionPlan.FREE, null)

        val claims = mapOf("exp" to Instant.now().epochSecond)
        assertThat(JwtUserSubscription.createFromJwtClaimMap(claims)).isEqualTo(expected)
    }

    @Test
    fun `createFromJwtClaimMap - no expire time`() {
        val expected = JwtUserSubscription(UserSubscriptionPlan.FREE, null)

        val claims = mapOf("plan" to "PRO")
        assertThat(JwtUserSubscription.createFromJwtClaimMap(claims)).isEqualTo(expected)
    }
}