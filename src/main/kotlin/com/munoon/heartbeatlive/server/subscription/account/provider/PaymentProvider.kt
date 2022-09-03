package com.munoon.heartbeatlive.server.subscription.account.provider

import com.munoon.heartbeatlive.server.subscription.account.model.PaymentProviderInfo
import com.munoon.heartbeatlive.server.user.User

interface PaymentProvider {
    val info: PaymentProviderInfo

    val providerName: PaymentProviderName

    suspend fun stopRenewingSubscription(user: User, details: User.Subscription.SubscriptionDetails)

    suspend fun makeARefund(user: User)
}