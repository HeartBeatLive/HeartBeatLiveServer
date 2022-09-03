package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class SubscriptionServiceMaintainLimitTest : AbstractTest() {
    @Autowired
    private lateinit var service: SubscriptionService

    @Autowired
    private lateinit var repository: SubscriptionRepository

    @Test
    fun `maintainMaxSubscribersLimit - lock limited`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now()
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(10)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(20)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true),
                created = Instant.now().minusSeconds(30)
            ))
        }

        val expectedSubscription2 = subscription2.copy(lock = Subscription.Lock(byPublisher = true))
        val expectedSubscription3 = subscription3.copy(lock = Subscription.Lock(byPublisher = true))

        runBlocking { service.maintainMaxSubscribersLimit("user1", newLimit = 1) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, expectedSubscription2, expectedSubscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `maintainMaxSubscribersLimit - unlock limited`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now()
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true),
                created = Instant.now().minusSeconds(10)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true, bySubscriber = true),
                created = Instant.now().minusSeconds(20)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true),
                created = Instant.now().minusSeconds(30)
            ))
        }

        val expectedSubscription2 = subscription2.copy(lock = Subscription.Lock(byPublisher = false))
        val expectedSubscription3 = subscription3.copy(lock = Subscription.Lock(bySubscriber = true))

        runBlocking { service.maintainMaxSubscribersLimit("user1", newLimit = 3) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, expectedSubscription2, expectedSubscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `maintainMaxSubscribersLimit - no action needed`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now()
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(10)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true),
                created = Instant.now().minusSeconds(20)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true, bySubscriber = true),
                created = Instant.now().minusSeconds(30)
            ))
        }

        runBlocking { service.maintainMaxSubscribersLimit("user1", newLimit = 2) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, subscription2, subscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `maintainMaxSubscriptionsLimit - lock limited`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(60)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(70)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(bySubscriber = true),
                created = Instant.now().minusSeconds(80)
            ))
        }

        val expectedSubscription2 = subscription2.copy(lock = Subscription.Lock(bySubscriber = true))
        val expectedSubscription3 = subscription3.copy(lock = Subscription.Lock(bySubscriber = true))

        runBlocking { service.maintainMaxSubscriptionsLimit("user2", 1) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, expectedSubscription2, expectedSubscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `maintainMaxSubscriptionsLimit - unlock limited`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(bySubscriber = true),
                created = Instant.now().minusSeconds(60)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(bySubscriber = true, byPublisher = true),
                created = Instant.now().minusSeconds(70)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(bySubscriber = true),
                created = Instant.now().minusSeconds(80)
            ))
        }

        val expectedSubscription2 = subscription2.copy(lock = Subscription.Lock(bySubscriber = false))
        val expectedSubscription3 = subscription3.copy(lock = Subscription.Lock(byPublisher = true))

        runBlocking { service.maintainMaxSubscriptionsLimit("user2", 3) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, expectedSubscription2, expectedSubscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `maintainMaxSubscriptionsLimit - no action need`() {
        val subscription1 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false
            ))
        }
        val subscription2 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                created = Instant.now().minusSeconds(60)
            ))
        }
        val subscription3 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(bySubscriber = true),
                created = Instant.now().minusSeconds(70)
            ))
        }
        val subscription4 = runBlocking {
            repository.save(Subscription(
                userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false,
                lock = Subscription.Lock(byPublisher = true, bySubscriber = true),
                created = Instant.now().minusSeconds(80)
            ))
        }

        runBlocking { service.maintainMaxSubscriptionsLimit("user2", 2) }

        val actual = runBlocking { repository.findAll().toList(arrayListOf()) }
        val expected = listOf(subscription1, subscription2, subscription3, subscription4)
        assertThat(actual).usingRecursiveFieldByFieldElementComparatorIgnoringFields("created").isEqualTo(expected)
    }
}