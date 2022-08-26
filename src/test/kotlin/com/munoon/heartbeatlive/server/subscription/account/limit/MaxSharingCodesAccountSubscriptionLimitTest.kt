package com.munoon.heartbeatlive.server.subscription.account.limit

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

internal class MaxSharingCodesAccountSubscriptionLimitTest : FreeSpec({
    val service = mockk<HeartBeatSharingService>()
    val properties = SubscriptionProperties().apply {
        subscription = mapOf(
            UserSubscriptionPlan.PRO to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSharingCodesLimit = 10
                }
            },
            UserSubscriptionPlan.FREE to SubscriptionProperties.Subscription().apply {
                limits = SubscriptionProperties.SubscriptionLimits().apply {
                    maxSharingCodesLimit = 5
                }
            }
        )
    }
    val limit = MaxSharingCodesAccountSubscriptionLimit(service, properties)

    "getCurrentLimit" {
        limit.getCurrentLimit(UserSubscriptionPlan.PRO) shouldBe 10
        limit.getCurrentLimit(UserSubscriptionPlan.FREE) shouldBe 5
    }

    "maintainALimit" {
        coEvery { service.maintainUserLimit(any(), any()) } returns Unit

        limit.maintainALimit("user1", UserSubscriptionPlan.PRO)
        coVerify(exactly = 1) { service.maintainUserLimit("user1", 10) }

        limit.maintainALimit("user2", UserSubscriptionPlan.FREE)
        coVerify(exactly = 1) { service.maintainUserLimit("user2", 5) }
    }
})