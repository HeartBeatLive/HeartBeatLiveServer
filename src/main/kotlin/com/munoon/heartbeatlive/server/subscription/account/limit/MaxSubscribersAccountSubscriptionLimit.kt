package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import org.springframework.stereotype.Component

@Component
class MaxSubscribersAccountSubscriptionLimit(
    private val subscriptionProperties: SubscriptionProperties,
    private val subscriptionService: SubscriptionService
) : AccountSubscriptionLimit<Int> {
    override fun getCurrentLimit(subscriptionPlan: UserSubscriptionPlan): Int {
        return subscriptionProperties[subscriptionPlan].limits.maxSubscribersLimit
    }

    override suspend fun maintainALimit(userId: String, newSubscriptionPlan: UserSubscriptionPlan) {
        subscriptionService.maintainMaxSubscribersLimit(userId, getCurrentLimit(newSubscriptionPlan))
    }
}