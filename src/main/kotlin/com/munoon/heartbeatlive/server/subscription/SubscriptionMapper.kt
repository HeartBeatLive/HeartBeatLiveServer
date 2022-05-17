package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo

object SubscriptionMapper {
    fun Subscription.asGraphQL() = GraphqlSubscriptionInfo(
        id = id!!,
        subscribeTime = created,
        userId = userId,
        subscriberUserId = subscriberUserId
    )
}