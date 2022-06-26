package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.SubscriptionUtils.validateUserSubscribersCount
import com.munoon.heartbeatlive.server.subscription.SubscriptionUtils.validateUserSubscriptionsCount
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

internal class SubscriptionUtilsTest {
    @Test
    fun `validateUserSubscribersCount - limit exceeded`() {
        val subscriptionService = mockk<SubscriptionService>() {
            coEvery { checkUserHaveMaximumSubscribers("user1") } returns true
        }

        assertThatThrownBy { runBlocking {
            subscriptionService.validateUserSubscribersCount("user1")
        } }.isExactlyInstanceOf(UserSubscribersLimitExceededException::class.java)

        coVerify(exactly = 1) { subscriptionService.checkUserHaveMaximumSubscribers("user1") }
    }

    @Test
    fun `validateUserSubscribersCount - limit not exceeded`() {
        val subscriptionService = mockk<SubscriptionService>() {
            coEvery { checkUserHaveMaximumSubscribers("user1") } returns false
        }

        assertThatNoException().isThrownBy { runBlocking {
            subscriptionService.validateUserSubscribersCount("user1")
        } }

        coVerify(exactly = 1) { subscriptionService.checkUserHaveMaximumSubscribers("user1") }
    }

    @Test
    fun `validateUserSubscriptionsCount - true`() {
        val subscriptionService = mockk<SubscriptionService>() {
            coEvery { checkUserHaveMaximumSubscriptions("user1", any()) } returns true
        }

        assertThatThrownBy { runBlocking {
            subscriptionService.validateUserSubscriptionsCount("user1", UserSubscriptionPlan.PRO)
        } }.isExactlyInstanceOf(UserSubscriptionsLimitExceededException::class.java)

        coVerify(exactly = 1) {
            subscriptionService.checkUserHaveMaximumSubscriptions("user1", UserSubscriptionPlan.PRO)
        }
    }

    @Test
    fun `validateUserSubscriptionsCount - false`() {
        val subscriptionService = mockk<SubscriptionService>() {
            coEvery { checkUserHaveMaximumSubscriptions("user1", UserSubscriptionPlan.FREE) } returns false
        }

        assertThatNoException().isThrownBy { runBlocking {
            subscriptionService.validateUserSubscriptionsCount("user1", UserSubscriptionPlan.FREE)
        } }

        coVerify(exactly = 1) {
            subscriptionService.checkUserHaveMaximumSubscriptions("user1", UserSubscriptionPlan.FREE)
        }
    }
}