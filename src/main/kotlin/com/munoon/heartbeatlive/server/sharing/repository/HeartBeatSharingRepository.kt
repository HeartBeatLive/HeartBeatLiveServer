package com.munoon.heartbeatlive.server.sharing.repository

import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.findIdsByUserId
import com.munoon.heartbeatlive.server.subscription.account.limit.AccountSubscriptionLimitUtils.lockAllById
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

interface HeartBeatSharingRepository : CoroutineSortingRepository<HeartBeatSharing, String>,
    CustomHeartBeatSharingRepository {

    suspend fun existsByPublicCode(publicCode: String): Boolean

    suspend fun findByPublicCode(publicCode: String): HeartBeatSharing?

    suspend fun deleteByIdAndUserId(id: String, userId: String): Int

    suspend fun deleteHeartBeatSharingById(id: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<HeartBeatSharing>

    suspend fun countAllByUserId(userId: String): Int

    suspend fun countAllByUserIdAndLockedTrue(userId: String): Int

    suspend fun deleteAllByUserId(userId: String)
}

interface CustomHeartBeatSharingRepository {
    suspend fun lockAllById(ids: Set<String>, lock: Boolean)

    suspend fun findIdsByUserId(userId: String, locked: Boolean, pageable: Pageable): Set<String>
}

@Repository
private class CustomHeartBeatSharingRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : CustomHeartBeatSharingRepository {
    override suspend fun lockAllById(ids: Set<String>, lock: Boolean) {
        mongoTemplate.lockAllById<HeartBeatSharing>(lockFieldName = "locked", ids, lock)
    }

    override suspend fun findIdsByUserId(userId: String, locked: Boolean, pageable: Pageable): Set<String> {
        return mongoTemplate.findIdsByUserId(
            collectionName = HeartBeatSharing.COLLECTION_NAME,
            userIdFieldName = "userId",
            lockedFieldName = "locked",
            userId, locked, pageable
        )
    }
}