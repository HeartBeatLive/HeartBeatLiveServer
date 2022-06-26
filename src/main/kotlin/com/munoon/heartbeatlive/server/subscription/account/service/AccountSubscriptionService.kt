package com.munoon.heartbeatlive.server.subscription.account.service

import com.munoon.heartbeatlive.server.subscription.account.PaymentProviderNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.provider.PaymentProvider
import org.springframework.stereotype.Service

@Service
class AccountSubscriptionService(private val providers: List<PaymentProvider>) {
    fun getPaymentProviderInfo(supportedProviders: Set<GraphqlPaymentProviderName>): PaymentProviderInfo {
        return providers.find { supportedProviders.contains(it.providerName) }?.info
            ?: throw PaymentProviderNotFoundException()
    }
}