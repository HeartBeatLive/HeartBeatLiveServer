package com.munoon.heartbeatlive.server.subscription.account.model

sealed interface GraphqlPaymentProvider {
    val name: GraphqlPaymentProviderName
}

data class GraphqlStripePaymentProvider(
    val publicApiKey: String
) : GraphqlPaymentProvider {
    override val name = GraphqlPaymentProviderName.STRIPE
}