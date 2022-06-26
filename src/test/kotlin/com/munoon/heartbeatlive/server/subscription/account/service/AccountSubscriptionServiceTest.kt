package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.UserHaveNotActiveSubscriptionException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.provider.StripePaymentProvider
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import java.time.Instant

class AccountSubscriptionServiceTest : FreeSpec({
    "getPaymentProviderInfo" - {
        "found" {
            val stripePaymentProvider = StripePaymentProvider(StripeConfigurationProperties().apply {
                publicApiKey = "publicApiKey"
            }, mockk())
            val service = AccountSubscriptionService(listOf(stripePaymentProvider), mockk())

            service.getPaymentProviderInfo(setOf(GraphqlPaymentProviderName.STRIPE)) shouldBe stripePaymentProvider.info
        }

        "not found" {
            val service = AccountSubscriptionService(emptyList(), mockk())
            shouldThrowExactly<PaymentProviderNotFoundException> {
                service.getPaymentProviderInfo(setOf(GraphqlPaymentProviderName.STRIPE))
            }
        }
    }

    "stopRenewingSubscription" - {
        "successful" {
            val userSubscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.now().plusSeconds(30),
                details = User.Subscription.StripeSubscriptionDetails("StripeSubscription1")
            )
            val user = User(
                id = "user1",
                displayName = null, email = null, emailVerified = false,
                subscription = userSubscription
            )
            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns user
            }
            val stripePaymentProvider = spyk(StripePaymentProvider(StripeConfigurationProperties(), mockk())) {
                coEvery { stopRenewingSubscription(any(), any()) } returns Unit
            }

            val service = AccountSubscriptionService(listOf(stripePaymentProvider), userService)

            service.stopRenewingSubscription("user1")

            coVerify(exactly = 1) { userService.getUserById("user1") }
            coVerify(exactly = 1) { stripePaymentProvider.stopRenewingSubscription(user, userSubscription.details) }
        }

        "user have no active subscription" {
            val user = User(
                id = "user1",
                displayName = null, email = null, emailVerified = false,
                subscription = null
            )
            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns user
            }

            val service = AccountSubscriptionService(emptyList(), userService)

            shouldThrowExactly<UserHaveNotActiveSubscriptionException> {
                service.stopRenewingSubscription("user1")
            }

            coVerify(exactly = 1) { userService.getUserById("user1") }
        }
    }
})