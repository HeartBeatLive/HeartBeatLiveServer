package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.subscription.service.UserSubscribersLoaderService
import com.ninjasquad.springmockk.SpykBean
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

@SpringBootTest
internal class HeartBeatSubscribersManagerTest : AbstractTest() {
    @Autowired
    private lateinit var heartBeatSubscribersManager: HeartBeatSubscribersManager

    @Autowired
    private lateinit var subscriptionRepository: SubscriptionRepository

    @SpykBean
    private lateinit var userSubscribersLoaderService: UserSubscribersLoaderService

    @Test
    fun sendHeartRate() {
        val subscription = runBlocking {
            subscriptionRepository.save(Subscription(userId = "user1", subscriberUserId = "user2",
                receiveHeartRateMatchNotifications = false))
        }

        val (user1Listener1Messages, user1Listener1) = heartBeatSubscribersManager.createSubscription("user1")
            .subscribeAsyncAndCollectMessages()
        val (user1Listener2Messages, user1Listener2) = heartBeatSubscribersManager.createSubscription("user1")
            .subscribeAsyncAndCollectMessages()
        val (user2Listener1Messages, user2Listener1) = heartBeatSubscribersManager.createSubscription("user2")
            .subscribeAsyncAndCollectMessages()
        val (user2Listener2Messages, user2Listener2) = heartBeatSubscribersManager.createSubscription("user2")
            .subscribeAsyncAndCollectMessages()
        val (user3Listener1Messages, user3Listener1) = heartBeatSubscribersManager.createSubscription("user3")
            .subscribeAsyncAndCollectMessages()
        val (user3Listener2Messages, user3Listener2) = heartBeatSubscribersManager.createSubscription("user3")
            .subscribeAsyncAndCollectMessages()

        runBlocking { heartBeatSubscribersManager.sendHeartRate("user1", 123.45f) }

        val user1ExpectedHeartRateInfo = HeartRateInfo(
            subscriptionId = null,
            heartRate = 123.45f,
            ownHeartRate = true
        )
        assertThat(user1Listener1Messages)
            .usingRecursiveComparison()
            .isEqualTo(listOf(user1ExpectedHeartRateInfo))
        assertThat(user1Listener2Messages)
            .usingRecursiveComparison()
            .isEqualTo(listOf(user1ExpectedHeartRateInfo))

        val user2ExpectedHeartRateInfo = HeartRateInfo(
            subscriptionId = subscription.id,
            heartRate = 123.45f,
            ownHeartRate = false
        )
        assertThat(user2Listener1Messages)
            .usingRecursiveComparison()
            .isEqualTo(listOf(user2ExpectedHeartRateInfo))
        assertThat(user2Listener2Messages)
            .usingRecursiveComparison()
            .isEqualTo(listOf(user2ExpectedHeartRateInfo))

        assertThat(user3Listener1Messages).isEmpty()
        assertThat(user3Listener2Messages).isEmpty()

        verify(exactly = 1) { userSubscribersLoaderService.load("user1") }

        user1Listener1.dispose()
        user1Listener2.dispose()
        user2Listener1.dispose()
        user2Listener2.dispose()
        user3Listener1.dispose()
        user3Listener2.dispose()
    }

    private companion object {
        fun Flux<HeartRateInfo>.subscribeAsyncAndCollectMessages(): Pair<List<HeartRateInfo>, Disposable> {
            val list = arrayListOf<HeartRateInfo>()
            val subscription = subscribeOn(Schedulers.parallel())
                .subscribe { list += it }
            return list to subscription
        }
    }
}