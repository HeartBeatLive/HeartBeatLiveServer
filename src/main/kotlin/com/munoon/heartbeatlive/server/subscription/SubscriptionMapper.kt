package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionLockInfo

object SubscriptionMapper {
    fun Subscription.asGraphQL() = GraphqlSubscriptionInfo(
        id = id!!,
        subscribeTime = created,
        userId = userId,
        subscriberUserId = subscriberUserId,
        lock = GraphqlSubscriptionLockInfo(
            locked = lock.byPublisher || lock.bySubscriber,
            byPublisher = lock.byPublisher,
            bySubscriber = lock.bySubscriber
        )
    )
}