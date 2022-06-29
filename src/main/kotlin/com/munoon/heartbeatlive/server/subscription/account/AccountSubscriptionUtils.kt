package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asSubscriptionJwt
import com.munoon.heartbeatlive.server.user.User
import java.time.Instant

object AccountSubscriptionUtils {
    fun getActiveSubscriptionPlan(subscription: UserSubscriptionPlan, expiration: Instant?) = when {
        expiration == null -> UserSubscriptionPlan.FREE
        Instant.now().isAfter(expiration) -> UserSubscriptionPlan.FREE
        else -> subscription
    }

    fun JwtUserSubscription.getActiveSubscriptionPlan() = getActiveSubscriptionPlan(plan, expirationTime)

    fun User.getActiveSubscriptionPlan() = asSubscriptionJwt().getActiveSubscriptionPlan()

    fun SubscriptionProperties.findSubscriptionPriceById(id: String): Pair<UserSubscriptionPlan, SubscriptionProperties.SubscriptionPrice> {
        subscription.entries.forEach { (plan, subscription) ->
            subscription.prices.forEach { price ->
                if (price.getId(plan) == id) return plan to price
            }
        }
        throw SubscriptionPlanPriceIsNotFoundByIdException(id)
    }
}