package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan

interface AccountSubscriptionLimit<T> {
    fun getCurrentLimit(subscriptionPlan: UserSubscriptionPlan): T

    /**
     * This function should maintain new user's limit.
     *
     * Example:
     * Subscription PRO give 20 sharing codes.
     * Subscription FREE give 5 sharing codes.
     *
     * User with subscription PRO have 15 sharing codes.
     * Then his subscription downgrade to FREE, and now he has only 5 available sharing codes.
     * This method should lock the rest 10 sharing codes.
     *
     * Then his subscription grow again to PRO. The previously locked 10 sharing codes should unlock now.
     */
    suspend fun maintainALimit(userId: String, newSubscriptionPlan: UserSubscriptionPlan)
}