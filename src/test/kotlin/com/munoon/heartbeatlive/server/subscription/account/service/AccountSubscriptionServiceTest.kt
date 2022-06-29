package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.RefundPeriodEndException
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
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

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
                startAt = Instant.now(),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "StripeSubscription1",
                    paymentIntentId = "PaymentIntent1"
                )
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

    "requestARefund" - {
        "successful" {
            val user = User(
                id = "user1",
                displayName = null,
                email = null,
                emailVerified = false,
                subscription = User.Subscription(
                    plan = UserSubscriptionPlan.PRO,
                    expiresAt = Instant.now().plusSeconds(120),
                    startAt = Instant.now(),
                    details = User.Subscription.StripeSubscriptionDetails(
                        subscriptionId = "StripeSubscription1",
                        paymentIntentId = "StripePaymentIntent1"
                    ),
                    refundDuration = Duration.ofDays(3)
                )
            )

            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns user
                coEvery { updateUserSubscription(any(), any()) } returns user
            }
            val stripePaymentProvider = spyk(StripePaymentProvider(StripeConfigurationProperties(), mockk())) {
                coEvery { makeARefund(any()) } returns Unit
            }

            val service = AccountSubscriptionService(listOf(stripePaymentProvider), userService)
            service.requestARefund("user1")

            coVerify(exactly = 1) { userService.getUserById("user1") }
            coVerify(exactly = 1) { stripePaymentProvider.makeARefund(user) }
            coVerify(exactly = 1) { userService.updateUserSubscription("user1", null) }
        }

        "user have no active subscription" {
            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns User(
                    id = "user1",
                    displayName = null,
                    email = null,
                    emailVerified = false,
                    subscription = null
                )
            }

            val service = AccountSubscriptionService(emptyList(), userService)
            shouldThrowExactly<UserHaveNotActiveSubscriptionException> {
                service.requestARefund("user1")
            }

            coVerify(exactly = 1) { userService.getUserById("user1") }
            coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
        }

        "refund period expired" {
            val userService = mockk<UserService>() {
                coEvery { getUserById(any()) } returns User(
                    id = "user1",
                    displayName = null,
                    email = null,
                    emailVerified = false,
                    subscription = User.Subscription(
                        plan = UserSubscriptionPlan.PRO,
                        expiresAt = OffsetDateTime.now().plusMonths(1).toInstant(),
                        startAt = OffsetDateTime.now().minusDays(10).toInstant(),
                        details = User.Subscription.StripeSubscriptionDetails(
                            subscriptionId = "StripeSubscription1",
                            paymentIntentId = "StripePaymentIntent1"
                        ),
                        refundDuration = Duration.ofDays(5)
                    )
                )
            }

            val service = AccountSubscriptionService(emptyList(), userService)
            shouldThrowExactly<RefundPeriodEndException> {
                service.requestARefund("user1")
            }

            coVerify(exactly = 1) { userService.getUserById("user1") }
            coVerify(exactly = 0) { userService.updateUserSubscription(any(), any()) }
        }
    }
})