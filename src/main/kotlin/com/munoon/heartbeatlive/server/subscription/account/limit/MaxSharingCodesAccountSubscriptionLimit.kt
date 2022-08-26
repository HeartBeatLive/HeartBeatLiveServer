package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import org.springframework.stereotype.Component

@Component
class MaxSharingCodesAccountSubscriptionLimit(
    private val service: HeartBeatSharingService,
    private val subscriptionProperties: SubscriptionProperties
) : AccountSubscriptionLimit<Int> {
    override fun getCurrentLimit(subscriptionPlan: UserSubscriptionPlan) =
        subscriptionProperties[subscriptionPlan].limits.maxSharingCodesLimit

    override suspend fun maintainALimit(userId: String, newSubscriptionPlan: UserSubscriptionPlan) {
        service.maintainUserLimit(userId, getCurrentLimit(newSubscriptionPlan))
    }
}