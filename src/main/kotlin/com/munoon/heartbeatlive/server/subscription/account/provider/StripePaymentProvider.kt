package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.config.properties.StripeConfigurationProperties
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("payment.stripe.enabled")
class StripePaymentProvider(
    private val properties: StripeConfigurationProperties
) : PaymentProvider {
    override val info: PaymentProviderInfo
        get() = StripePaymentProviderInfo(publicKey = properties.publicApiKey)

    override val providerName = GraphqlPaymentProviderName.STRIPE
}