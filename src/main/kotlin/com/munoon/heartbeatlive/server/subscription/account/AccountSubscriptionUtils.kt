package com.munoon.heartbeatlive.server.subscription.account

import java.time.Instant

object AccountSubscriptionUtils {
    fun getActiveSubscriptionPlan(subscription: UserSubscriptionPlan, expiration: Instant?) = when {
        expiration == null -> UserSubscriptionPlan.FREE
        Instant.now().isAfter(expiration) -> UserSubscriptionPlan.FREE
        else -> subscription
    }

    fun JwtUserSubscription.getActiveSubscriptionPlan() = getActiveSubscriptionPlan(plan, expirationTime)
}