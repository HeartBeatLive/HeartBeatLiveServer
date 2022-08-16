package com.munoon.heartbeatlive.server.subscription.account.stripe

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("stripeRecurringChargeFailure")
data class StripeRecurringChargeFailure(
    @Id
    val userId: String,

    val stripeInvoiceId: String,

    val clientSecret: String,

    val paymentIntentStatus: String,

    val created: Instant = Instant.now(),

    val expiresAt: Instant
)
