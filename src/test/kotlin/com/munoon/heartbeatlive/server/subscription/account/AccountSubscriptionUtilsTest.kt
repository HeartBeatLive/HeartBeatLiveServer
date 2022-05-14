package com.munoon.heartbeatlive.server.subscription.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class AccountSubscriptionUtilsTest {
    @Test
    fun `getActiveSubscriptionPlan - current subscription`() {
        val expirationTime = Instant.now() + Duration.ofDays(10)
        val activeSubscription = AccountSubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.PRO)
    }

    @Test
    fun `getActiveSubscriptionPlan - subscription expired`() {
        val expirationTime = Instant.now() - Duration.ofDays(10)
        val activeSubscription = AccountSubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
    }

    @Test
    fun `getActiveSubscriptionPlan - expiration time not specified`() {
        val activeSubscription = AccountSubscriptionUtils.getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, null)
        assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
    }
}