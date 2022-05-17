package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService

object SubscriptionUtils {
    suspend fun SubscriptionService.validateUserSubscribersCount(userId: String) {
        if (checkUserHaveMaximumSubscribers(userId)) {
            throw UserSubscribersLimitExceededException()
        }
    }

    suspend fun SubscriptionService.validateUserSubscriptionsCount(userId: String) {
        if (checkUserHaveMaximumSubscriptions(userId)) {
            throw UserSubscriptionsLimitExceededException()
        }
    }
}