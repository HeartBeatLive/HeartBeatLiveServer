package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.provider.StripePaymentProvider
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class AccountSubscriptionServiceTest : FreeSpec({
    "getPaymentProviderInfo" - {
        "found" {
            val stripePaymentProvider = StripePaymentProvider(StripeConfigurationProperties().apply {
                publicApiKey = "publicApiKey"
            })
            val service = AccountSubscriptionService(listOf(stripePaymentProvider))

            service.getPaymentProviderInfo(setOf(GraphqlPaymentProviderName.STRIPE)) shouldBe stripePaymentProvider.info
        }

        "not found" {
            val service = AccountSubscriptionService(emptyList())
            shouldThrowExactly<PaymentProviderNotFoundException> {
                service.getPaymentProviderInfo(setOf(GraphqlPaymentProviderName.STRIPE))
            }
        }
    }
})