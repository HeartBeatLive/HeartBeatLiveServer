package com.munoon.heartbeatlive.server.subscription.model

data class GraphqlSubscriptionLockInfo(
    val locked: Boolean,
    val byPublisher: Boolean,
    val bySubscriber: Boolean
)