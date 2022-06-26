package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo

interface PaymentProvider {
    val info: PaymentProviderInfo

    val providerName: GraphqlPaymentProviderName
}