package com.munoon.heartbeatlive.server.subscription.account.stripe

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("stripeAccount")
data class StripeAccount(
    @Id
    val id: String,

    val stripeAccountId: String
)