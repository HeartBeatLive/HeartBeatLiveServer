package com.munoon.heartbeatlive.server.subscription.account.model

sealed interface PaymentProviderInfo

data class StripePaymentProviderInfo(
    val publicKey: String
) : PaymentProviderInfo