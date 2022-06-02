package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.common.CacheUtils.cache2kBuilder
import com.munoon.heartbeatlive.server.config.properties.CacheProperties
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.cache2k.addon.UniversalResiliencePolicy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class UserSubscribersLoaderService(
    private val repository: SubscriptionRepository,
    cacheProperties: CacheProperties
) {
    private val logger = LoggerFactory.getLogger(UserSubscribersLoaderService::class.java)
    private val cache = cache2kBuilder<String, Map<String, String>>()
        .name("userSubscribersCache")
        .loader { userId ->
            runBlocking { repository.findAllByUserId(userId).toList(arrayListOf()) }
                .associate { it.id!! to it.subscriberUserId }
        }
        .idleScanTime(cacheProperties.userSubscribers.idleScanTime)
        .entryCapacity(cacheProperties.userSubscribers.entryCapacity)
        .setupWith({ UniversalResiliencePolicy.enable(it) }) {
            it.retryInterval(15, TimeUnit.SECONDS)
        }
        .build()

    // load user's subscribers and return as map of [subscription id -> subscriber user id]
    fun load(userId: String): Map<String, String> {
        return try {
            cache.get(userId) ?: emptyMap()
        } catch (e: Exception) {
            logger.error("Error loading user '$userId' subscribers", e)
            emptyMap()
        }
    }
}