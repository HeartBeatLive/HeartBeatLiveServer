package com.munoon.heartbeatlive.server.subscription

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class SubscriptionUtilsTest {
    @Test
    fun `getActiveSubscriptionPlan - current subscription`() {
        val expirationTime = Instant.now() + Duration.ofDays(10)
        val activeSubscription = SubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.PRO)
    }

    @Test
    fun `getActiveSubscriptionPlan - subscription expired`() {
        val expirationTime = Instant.now() - Duration.ofDays(10)
        val activeSubscription = SubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
    }

    @Test
    fun `getActiveSubscriptionPlan - expiration time not specified`() {
        val activeSubscription = SubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, null)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
    }
}