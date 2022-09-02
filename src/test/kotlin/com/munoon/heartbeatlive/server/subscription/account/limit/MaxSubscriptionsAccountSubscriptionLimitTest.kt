package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

internal class MaxSubscriptionsAccountSubscriptionLimitTest : FreeSpec({
    val properties = SubscriptionProperties().apply {
        subscription = mapOf(
            UserSubscriptionPlan.FREE to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSubscriptionsLimit = 1
                }
            },
            UserSubscriptionPlan.PRO to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSubscriptionsLimit = 5
                }
            }
        )
    }
    val service = mockk<SubscriptionService>()
    val limit = MaxSubscriptionsAccountSubscriptionLimit(properties, service)

    "getCurrentLimit" {
        limit.getCurrentLimit(UserSubscriptionPlan.FREE) shouldBe 1
        limit.getCurrentLimit(UserSubscriptionPlan.PRO) shouldBe 5
    }

    "maintainALimit" {
        coEvery { service.maintainMaxSubscriptionsLimit(any(), any()) } returns Unit

        limit.maintainALimit("user1", UserSubscriptionPlan.PRO)
        coVerify(exactly = 1) { service.maintainMaxSubscriptionsLimit("user1", 5) }

        limit.maintainALimit("user2", UserSubscriptionPlan.FREE)
        coVerify(exactly = 1) { service.maintainMaxSubscriptionsLimit("user2", 1) }
    }
})