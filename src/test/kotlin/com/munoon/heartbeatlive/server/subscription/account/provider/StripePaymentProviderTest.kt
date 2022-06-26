package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class StripePaymentProviderTest : FreeSpec({
    val properties = StripeConfigurationProperties().apply {
        enabled = true
        publicApiKey = "stripePublicApiKey"
    }
    val provider = StripePaymentProvider(properties)

    "info" {
        provider.info shouldBe StripePaymentProviderInfo("stripePublicApiKey")
    }

    "providerName" {
        provider.providerName shouldBe GraphqlPaymentProviderName.STRIPE
    }
})