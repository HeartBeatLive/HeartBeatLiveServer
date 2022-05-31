package com.munoon.heartbeatlive.server.heartrate.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.heartrate.HeartBeatSubscribersManager
import com.munoon.heartbeatlive.server.heartrate.HeartRateSubscriber
import com.munoon.heartbeatlive.server.heartrate.LocalSubscribersSenderHeartRateInfoHandler
import com.munoon.heartbeatlive.server.heartrate.MessagePublisherHeartRateInfoHandler
import com.munoon.heartbeatlive.server.heartrate.TooManyHeartRateSubscriptionsExceptions
import com.munoon.heartbeatlive.server.heartrate.UserStatusUpdaterHeartRateInfoHandler
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import com.munoon.heartbeatlive.server.heartrate.repository.HeartRateSubscriberRepository
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.task.AsyncTaskExecutor
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest
internal class HeartRateServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: HeartRateService

    @Autowired
    private lateinit var repository: HeartRateSubscriberRepository

    @MockkBean
    private lateinit var subscribersManager: HeartBeatSubscribersManager

    @SpykBean
    private lateinit var asyncTaskExecutor: AsyncTaskExecutor

    @MockkBean
    private lateinit var messagePublisherHeartRateInfoHandler: MessagePublisherHeartRateInfoHandler

    @MockkBean
    private lateinit var localSubscribersSenderHeartRateInfoHandler: LocalSubscribersSenderHeartRateInfoHandler

    @MockkBean
    private lateinit var userStatusUpdaterHeartRateInfoHandler: UserStatusUpdaterHeartRateInfoHandler

    @Test
    fun sendHeartRate() {
        every { messagePublisherHeartRateInfoHandler.handleHeartRateInfo(any(), any()) } returns Unit
        every { localSubscribersSenderHeartRateInfoHandler.handleHeartRateInfo(any(), any()) } returns Unit
        every { userStatusUpdaterHeartRateInfoHandler.handleHeartRateInfo(any(), any()) } returns Unit

        runBlocking { service.sendHeartRate("user1", 123.45f) }

        coVerify(exactly = 1, timeout = 60000) {
            messagePublisherHeartRateInfoHandler.handleHeartRateInfo("user1", 123.45f)
        }
        coVerify(exactly = 1, timeout = 60000) {
            localSubscribersSenderHeartRateInfoHandler.handleHeartRateInfo("user1", 123.45f)
        }
        coVerify(exactly = 1, timeout = 60000) {
            userStatusUpdaterHeartRateInfoHandler.handleHeartRateInfo("user1", 123.45f)
        }
        coVerify(exactly = 3) { asyncTaskExecutor.execute(any()) }
    }

    @Test
    fun subscribeToHeartRates() {
        val heartRateInfo = HeartRateInfo(subscriptionId = null, heartRate = 123.45f, ownHeartRate = true)
        every { subscribersManager.createSubscription(any()) } returns Flux.create { it.next(heartRateInfo) }

        runBlocking { assertThat(repository.count()).isZero }

        val heartRateInfoFuture = CompletableFuture<HeartRateInfo>()
        val result = service.subscribeToHeartRates("user1")
            .subscribeOn(Schedulers.parallel())
            .subscribe { heartRateInfoFuture.complete(it) }

        assertThat(heartRateInfoFuture.get(1, TimeUnit.MINUTES))
            .usingRecursiveComparison()
            .isEqualTo(heartRateInfo)

        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("id", "subscriptionTime")
                .isEqualTo(listOf(HeartRateSubscriber(userId = "user1")))
        }

        result.dispose()
        runBlocking { assertThat(repository.count()).isZero }
        verify(exactly = 1) { subscribersManager.createSubscription("user1") }
    }

    @Test
    fun `subscribeToHeartRates - too many subscriptions`() {
        every { subscribersManager.createSubscription(any()) } returns Flux.create { }
        val subscribers = (1..20).map { HeartRateSubscriber(userId = "user1") }
        runBlocking { repository.saveAll(subscribers).toList(arrayListOf()) }

        assertThatThrownBy { service.subscribeToHeartRates("user1").blockFirst() }
            .isEqualTo(TooManyHeartRateSubscriptionsExceptions("user1", 15))

        verify(exactly = 0) { subscribersManager.createSubscription(any()) }
    }
}