package com.munoon.heartbeatlive.server.subscription.account

data class AccountSubscription(
    val userId: String,
    val subscriptionPlan: UserSubscriptionPlan
)