package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.stripe.service.StripeAccountSubscriptionService
import com.munoon.heartbeatlive.server.user.User
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
                details = User.Subscription.StripeSubscriptionDetails(subscriptionId = "StripeSubscription1")
            )
        )

        provider.stopRenewingSubscription(user, user.subscription!!.details)

        coVerify(exactly = 1) { service.cancelUserSubscription("StripeSubscription1") }
    }
})