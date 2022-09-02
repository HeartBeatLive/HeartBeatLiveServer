package com.munoon.heartbeatlive.server.subscription.repository

import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.account.limit.SimpleAccountSubscriptionLimitRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Suppress("TooManyFunctions")
interface SubscriptionRepository : CoroutineSortingRepository<Subscription, String> {
    suspend fun countAllByUserId(userId: String): Int

    suspend fun findByUserIdAndSubscriberUserId(userId: String, subscriberUserId: String): Subscription?

    suspend fun deleteSubscriptionById(id: String): Int

    suspend fun deleteSubscriptionByIdAndSubscriberUserId(id: String, subscriberUserId: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<Subscription>

    fun findAllBySubscriberUserId(subscriberUserId: String, pageable: Pageable): Flux<Subscription>

    suspend fun countAllBySubscriberUserId(subscriberUserId: String): Int

    @Query("{ userId: ?0, lock: { byPublisher: false, bySubscriber: false } }")
    fun findAllByUserIdAndUnlocked(userId: String): Flow<Subscription>

    @Query("{ \$or: [ { userId: ?0 }, { subscriberUserId: ?0 } ] }", delete = true)
    suspend fun deleteAllByUserIdOrSubscriberUserId(userId: String)

    @Query("{ subscriberUserId: ?0, userId: ?1 }", delete = true)
    suspend fun deleteAllBySubscriberUserIdAndUserId(subscriberUserId: String, userId: String)

    @Query("{ subscriberUserId: ?0, 'lock.bySubscriber': true }", count = true)
    suspend fun countAllLockedSubscriptions(subscriberUserId: String): Int

    @Query("{ userId: ?0, 'lock.byPublisher': true }", count = true)
    suspend fun countAllLockedSubscribers(userId: String): Int
}

@Repository
class SubscriptionMaxSubscriptionsLimitRepository(
    mongoTemplate: ReactiveMongoTemplate,
    private val subscriptionRepository: SubscriptionRepository
) : SimpleAccountSubscriptionLimitRepository(
    mongoTemplate,
    collectionName = "subscription",
    userIdFieldName = "subscriberUserId",
    lockedFieldName = "lock.bySubscriber"
) {
    override suspend fun countAllByUserId(userId: String) =
        subscriptionRepository.countAllBySubscriberUserId(userId)

    override suspend fun countAllByUserIdAndLockedTrue(userId: String) =
        subscriptionRepository.countAllLockedSubscriptions(userId)
}

@Repository
class SubscriptionMaxSubscribersLimitRepository(
    mongoTemplate: ReactiveMongoTemplate,
    private val subscriptionRepository: SubscriptionRepository
) : SimpleAccountSubscriptionLimitRepository(
    mongoTemplate,
    collectionName = "subscription",
    userIdFieldName = "userId",
    lockedFieldName = "lock.byPublisher"
) {
    override suspend fun countAllByUserId(userId: String) =
        subscriptionRepository.countAllByUserId(userId)

    override suspend fun countAllByUserIdAndLockedTrue(userId: String) =
        subscriptionRepository.countAllLockedSubscribers(userId)
}