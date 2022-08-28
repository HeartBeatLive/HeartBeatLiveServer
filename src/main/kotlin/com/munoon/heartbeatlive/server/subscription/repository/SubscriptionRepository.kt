package com.munoon.heartbeatlive.server.subscription.repository

import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.findIdsByUserId
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.lockAllById
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

interface SubscriptionRepository : CoroutineSortingRepository<Subscription, String>, CustomSubscriptionRepository {
    suspend fun countAllByUserId(userId: String): Int

    suspend fun findByUserIdAndSubscriberUserId(userId: String, subscriberUserId: String): Subscription?

    suspend fun deleteSubscriptionById(id: String): Int

    suspend fun deleteSubscriptionByIdAndSubscriberUserId(id: String, subscriberUserId: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<Subscription>

    fun findAllBySubscriberUserId(subscriberUserId: String, pageable: Pageable): Flux<Subscription>

    suspend fun countAllBySubscriberUserId(subscriberUserId: String): Int

    fun findAllByUserIdAndLockedFalse(userId: String): Flow<Subscription>

    @Query("{ \$or: [ { userId: ?0 }, { subscriberUserId: ?0 } ] }", delete = true)
    suspend fun deleteAllByUserIdOrSubscriberUserId(userId: String)

    @Query("{ subscriberUserId: ?0, userId: ?1 }", delete = true)
    suspend fun deleteAllBySubscriberUserIdAndUserId(subscriberUserId: String, userId: String)

    suspend fun countAllBySubscriberUserIdAndLockedTrue(userId: String): Int
}

interface CustomSubscriptionRepository {
    suspend fun findIdsBySubscriberUserId(userId: String, locked: Boolean, pageable: Pageable): Set<String>

    suspend fun lockAllById(ids: Set<String>, lock: Boolean)
}

@Repository
class CustomSubscriptionRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : CustomSubscriptionRepository {
    override suspend fun findIdsBySubscriberUserId(userId: String, locked: Boolean, pageable: Pageable): Set<String> {
        return mongoTemplate.findIdsByUserId(
            collectionName = "subscription",
            userIdFieldName = "subscriberUserId",
            lockedFieldName = "locked",
            userId, locked, pageable
        )
    }

    override suspend fun lockAllById(ids: Set<String>, lock: Boolean) {
        return mongoTemplate.lockAllById<Subscription>(lockFieldName = "locked", ids, lock)
    }
}