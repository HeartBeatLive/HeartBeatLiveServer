package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.subscription.account.AccountSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import org.springframework.stereotype.Service

// mocked implementation for time
@Service
class AccountSubscriptionService {
    suspend fun getAccountSubscriptionByUserId(userId: String): AccountSubscription {
        return AccountSubscription(userId, UserSubscriptionPlan.FREE)
    }
}