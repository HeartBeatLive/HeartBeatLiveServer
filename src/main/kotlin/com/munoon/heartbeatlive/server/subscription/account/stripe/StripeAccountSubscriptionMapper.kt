package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.stripe.model.Subscription

object StripeAccountSubscriptionMapper {
    fun Subscription.asGraphql() = GraphqlStripeSubscription(
        subscriptionId = id,
        clientSecret = latestInvoiceObject.paymentIntentObject.clientSecret
    )
}