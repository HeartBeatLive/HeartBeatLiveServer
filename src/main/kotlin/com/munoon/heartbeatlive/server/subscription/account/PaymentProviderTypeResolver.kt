package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.config.AbstractGraphqlTypeNameResolver
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlStripePaymentProvider
import org.springframework.stereotype.Component

@Component
class PaymentProviderTypeResolver : AbstractGraphqlTypeNameResolver<GraphqlPaymentProvider>("PaymentProvider") {
    override fun getTypeName(obj: GraphqlPaymentProvider) = when (obj) {
        is GraphqlStripePaymentProvider -> "StripePaymentProvider"
    }
}