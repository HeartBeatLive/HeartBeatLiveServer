package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk

internal class MaxSubscribersAccountSubscriptionLimitTest : FreeSpec({
    val service = mockk<SubscriptionService>(relaxUnitFun = true)
    val properties = SubscriptionProperties().apply {
        subscription = mapOf(
            UserSubscriptionPlan.PRO to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSubscribersLimit = 5
                }
            },
            UserSubscriptionPlan.FREE to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSubscribersLimit = 3
                }
            }
        )
    }
    val limit = MaxSubscribersAccountSubscriptionLimit(properties, service)

    "getCurrentLimit" {
        limit.getCurrentLimit(UserSubscriptionPlan.PRO) shouldBe 5
        limit.getCurrentLimit(UserSubscriptionPlan.FREE) shouldBe 3
    }

    "maintainALimit" {
        limit.maintainALimit("user1", UserSubscriptionPlan.PRO)
        coVerify(exactly = 1) { service.maintainMaxSubscriptionLimit("user1", 5) }

        limit.maintainALimit("user1", UserSubscriptionPlan.FREE)
        coVerify(exactly = 1) { service.maintainMaxSubscriptionLimit("user1", 3) }
    }
})