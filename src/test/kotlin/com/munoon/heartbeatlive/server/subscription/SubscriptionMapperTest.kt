package com.munoon.heartbeatlive.server.subscription

import com.munoon.heartbeatlive.server.subscription.SubscriptionMapper.asGraphQL
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionLockInfo
import io.kotest.core.spec.style.FreeSpec
import org.assertj.core.api.Assertions.assertThat
import java.time.Instant

internal class SubscriptionMapperTest : FreeSpec({
    "asGraphQL" - {
        "locked by subscriber" {
            val subscribeTime = Instant.now()

            val expected = GraphqlSubscriptionInfo(
                id = "subscription1",
                subscribeTime = subscribeTime,
                userId = "user1",
                subscriberUserId = "user2",
                lock = GraphqlSubscriptionLockInfo(
                    locked = true,
                    byPublisher = false,
                    bySubscriber = true
                )
            )

            val subscription = Subscription(
                id = "subscription1",
                userId = "user1",
                subscriberUserId = "user2",
                created = subscribeTime,
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = false, bySubscriber = true)
            )

            val actual = subscription.asGraphQL()
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        "locked by published" {
            val subscribeTime = Instant.now()

            val expected = GraphqlSubscriptionInfo(
                id = "subscription1",
                subscribeTime = subscribeTime,
                userId = "user1",
                subscriberUserId = "user2",
                lock = GraphqlSubscriptionLockInfo(
                    locked = true,
                    byPublisher = true,
                    bySubscriber = false
                )
            )

            val subscription = Subscription(
                id = "subscription1",
                userId = "user1",
                subscriberUserId = "user2",
                created = subscribeTime,
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true, bySubscriber = false)
            )

            val actual = subscription.asGraphQL()
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        "locked by both" {
            val subscribeTime = Instant.now()

            val expected = GraphqlSubscriptionInfo(
                id = "subscription1",
                subscribeTime = subscribeTime,
                userId = "user1",
                subscriberUserId = "user2",
                lock = GraphqlSubscriptionLockInfo(
                    locked = true,
                    byPublisher = true,
                    bySubscriber = true
                )
            )

            val subscription = Subscription(
                id = "subscription1",
                userId = "user1",
                subscriberUserId = "user2",
                created = subscribeTime,
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true, bySubscriber = true)
            )

            val actual = subscription.asGraphQL()
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        "unlocked" {
            val subscribeTime = Instant.now()

            val expected = GraphqlSubscriptionInfo(
                id = "subscription1",
                subscribeTime = subscribeTime,
                userId = "user1",
                subscriberUserId = "user2",
                lock = GraphqlSubscriptionLockInfo(
                    locked = false,
                    byPublisher = false,
                    bySubscriber = false
                )
            )

            val subscription = Subscription(
                id = "subscription1",
                userId = "user1",
                subscriberUserId = "user2",
                created = subscribeTime,
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = false, bySubscriber = false)
            )

            val actual = subscription.asGraphQL()
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }
    }
})