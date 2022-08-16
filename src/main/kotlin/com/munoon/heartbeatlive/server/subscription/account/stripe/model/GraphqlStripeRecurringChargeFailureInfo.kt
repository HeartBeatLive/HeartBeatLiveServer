package com.munoon.heartbeatlive.server.subscription.account.stripe.model

import java.time.Instant

data class GraphqlStripeRecurringChargeFailureInfo(
    val createTime: Instant,
    val expireTime: Instant,
    val clientSecret: String,
    val failureType: FailureType
) {
    enum class FailureType {
        REQUIRES_ACTION,
        REQUIRES_PAYMENT_METHOD
    }
}