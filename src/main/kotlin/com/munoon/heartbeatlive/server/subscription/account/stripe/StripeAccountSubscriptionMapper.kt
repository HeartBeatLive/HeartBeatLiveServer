package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeRecurringChargeFailureInfo
import com.munoon.heartbeatlive.server.subscription.account.stripe.model.GraphqlStripeSubscription
import com.stripe.model.Subscription

object StripeAccountSubscriptionMapper {
    fun Subscription.asGraphql() = GraphqlStripeSubscription(
        clientSecret = latestInvoiceObject.paymentIntentObject.clientSecret
    )

    fun StripeRecurringChargeFailure.asGraphql() = GraphqlStripeRecurringChargeFailureInfo(
        createTime = created,
        expireTime = expiresAt,
        clientSecret = clientSecret,
        failureType = when (paymentIntentStatus) {
            "requires_action" -> GraphqlStripeRecurringChargeFailureInfo.FailureType.REQUIRES_ACTION
            "requires_payment_method" -> GraphqlStripeRecurringChargeFailureInfo.FailureType.REQUIRES_PAYMENT_METHOD
            else -> throw IllegalStateException("Unknown payment intent status: $paymentIntentStatus")
        }
    )
}