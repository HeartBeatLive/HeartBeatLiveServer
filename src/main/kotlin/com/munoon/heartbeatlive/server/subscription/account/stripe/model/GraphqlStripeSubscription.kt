package com.munoon.heartbeatlive.server.subscription.account.stripe.model

data class GraphqlStripeSubscription(
    val subscriptionId: String,
    val clientSecret: String
)