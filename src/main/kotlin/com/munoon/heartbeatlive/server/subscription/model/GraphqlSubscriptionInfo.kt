package com.munoon.heartbeatlive.server.subscription.model

import java.time.Instant

data class GraphqlSubscriptionInfo(
    val id: String,
    val subscribeTime: Instant,
    val lock: GraphqlSubscriptionLockInfo,

    // for mapping only
    val userId: String,
    val subscriberUserId: String
)
