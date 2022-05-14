package com.munoon.heartbeatlive.server.subscription.repository

import com.munoon.heartbeatlive.server.subscription.Subscription
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import reactor.core.publisher.Flux

interface SubscriptionRepository : CoroutineSortingRepository<Subscription, String> {
    suspend fun countAllByUserId(userId: String): Int

    suspend fun findByUserIdAndSubscriberUserId(userId: String, subscriberUserId: String): Subscription?

    suspend fun deleteSubscriptionById(id: String): Int

    suspend fun deleteSubscriptionByIdAndSubscriberUserId(id: String, subscriberUserId: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<Subscription>

    fun findAllBySubscriberUserId(subscriberUserId: String, pageable: Pageable): Flux<Subscription>

    suspend fun countAllBySubscriberUserId(subscriberUserId: String): Int

    @Query("{ \$or: [ { userId: ?0 }, { subscriberUserId: ?0 } ] }", delete = true)
    suspend fun deleteAllByUserIdOrSubscriberUserId(userId: String)
}