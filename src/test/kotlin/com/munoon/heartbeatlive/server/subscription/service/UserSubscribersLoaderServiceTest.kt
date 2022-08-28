package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.config.properties.CacheProperties
import com.munoon.heartbeatlive.server.subscription.Subscription
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
        val service = mockk<SubscriptionService>() {
            coEvery { getAllActiveUserSubscribers(any()) } returns flowOf(
                Subscription(id = "subscription1", userId = "user1", subscriberUserId = "user2",
                    receiveHeartRateMatchNotifications = false),
                Subscription(id = "subscription2", userId = "user1", subscriberUserId = "user3",
                    receiveHeartRateMatchNotifications = false)
            )
        }
        val loader = UserSubscribersLoaderService(service, cacheProperties)

        listOf(
            loader.load("user1"),
            loader.load("user1"), // should be cached
            loader.load("user1") // should be cached
        ).forEach {
            assertThat(it).usingRecursiveComparison().isEqualTo(mapOf(
                "subscription1" to "user2",
                "subscription2" to "user3"
            ))
        }

        coVerify(exactly = 1) { service.getAllActiveUserSubscribers("user1") }
    }

    @Test
    fun `load - repository exception`() {
        val service = mockk<SubscriptionService>() {
            coEvery { getAllActiveUserSubscribers(any()) } throws RuntimeException("Repository exception")
        }
        val loader = UserSubscribersLoaderService(service, cacheProperties)

        listOf(
            loader.load("user1"),
            loader.load("user1"), // should be cached
            loader.load("user1") // should be cached
        ).forEach {
            assertThat(it).usingRecursiveComparison()
                .isEqualTo(emptyMap<String, String>())
        }

        coVerify(exactly = 1) { service.getAllActiveUserSubscribers("user1") }
    }
}