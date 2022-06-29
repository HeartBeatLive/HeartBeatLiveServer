package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.stripe.param.RefundCreateParams
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Duration
import java.time.Instant

class StripePaymentProviderTest : FreeSpec({
    val properties = StripeConfigurationProperties().apply {
        enabled = true
        publicApiKey = "stripePublicApiKey"
    }
    val service = mockk<StripeAccountSubscriptionService>()
    val provider = StripePaymentProvider(properties, service)

    "info" {
        provider.info shouldBe StripePaymentProviderInfo("stripePublicApiKey")
    }

    "providerName" {
        provider.providerName shouldBe PaymentProviderName.STRIPE
    }

    "stopRenewingSubscription" {
        coEvery { service.cancelUserSubscription(any()) } returns Unit

        val user = User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false,
            subscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.now().plusSeconds(60),
                startAt = Instant.now(),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "StripeSubscription1",
                    paymentIntentId = "PaymentIntent1"
                )
            )
        )

        provider.stopRenewingSubscription(user, user.subscription!!.details)

        coVerify(exactly = 1) { service.cancelUserSubscription("StripeSubscription1") }
    }

    "makeARefund" {
        coEvery { service.makeARefund(any(), any(), any()) } returns Unit

        val user = User(
            id = "user1",
            displayName = null,
            email = null, emailVerified = false,
            subscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.now().plusSeconds(90),
                startAt = Instant.now(),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "StripeSubscription1",
                    paymentIntentId = "StripePaymentIntent1"
                )
            )
        )

        provider.makeARefund(user)

        coVerify(exactly = 1) { service.makeARefund(
            subscriptionId = "StripeSubscription1",
            paymentIntentId = "StripePaymentIntent1",
            reason = RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER
        ) }
    }
})