package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.config.properties.CacheProperties
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.cache2k.CacheManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class UserSubscribersLoaderServiceTest {
    private companion object {
        val cacheProperties = CacheProperties().apply {
            userSubscribers = CacheProperties.UserSubscribersCacheSettings(
                entryCapacity = 1000,
                idleScanTime = Duration.ofSeconds(30)
            )
        }
    }

    @AfterEach
    @BeforeEach
    fun clearCaches() {
        CacheManager.closeAll()
    }

    @Test
    fun load() {
        val repository = mockk<SubscriptionRepository>() {
            coEvery { findAllByUserId(any()) } returns flowOf(
                Subscription(id = "subscription1", userId = "user1", subscriberUserId = "user2",
                    receiveHeartRateMatchNotifications = false),
                Subscription(id = "subscription2", userId = "user1", subscriberUserId = "user3",
                    receiveHeartRateMatchNotifications = false)
            )
        }
        val service = UserSubscribersLoaderService(repository, cacheProperties)

        listOf(
            service.load("user1"),
            service.load("user1"), // should be cached
            service.load("user1") // should be cached
        ).forEach {
            assertThat(it).usingRecursiveComparison().isEqualTo(mapOf(
                "subscription1" to "user2",
                "subscription2" to "user3"
            ))
        }

        coVerify(exactly = 1) { repository.findAllByUserId("user1") }
    }

    @Test
    fun `load - repository exception`() {
        val repository = mockk<SubscriptionRepository>() {
            coEvery { findAllByUserId(any()) } throws RuntimeException("Repository exception")
        }
        val service = UserSubscribersLoaderService(repository, cacheProperties)

        listOf(
            service.load("user1"),
            service.load("user1"), // should be cached
            service.load("user1") // should be cached
        ).forEach {
            assertThat(it).usingRecursiveComparison()
                .isEqualTo(emptyMap<String, String>())
        }

        coVerify(exactly = 1) { repository.findAllByUserId("user1") }
    }
}