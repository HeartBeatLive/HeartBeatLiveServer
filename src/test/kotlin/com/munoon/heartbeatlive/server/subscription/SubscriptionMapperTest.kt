package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.SubscriptionMapper.asGraphQL
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.Instant

internal class SubscriptionMapperTest {
    @Test
    fun asGraphQL() {
        val subscribeTime = Instant.now()

        val expected = GraphqlSubscriptionInfo(
            id = "subscription1",
            subscribeTime = subscribeTime,
            userId = "user1",
            subscriberUserId = "user2",
            locked = true
        )

        val subscription = Subscription(
            id = "subscription1",
            userId = "user1",
            subscriberUserId = "user2",
            created = subscribeTime,
            receiveHeartRateMatchNotifications = false,
            locked = true
        )

        val actual = subscription.asGraphQL()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}