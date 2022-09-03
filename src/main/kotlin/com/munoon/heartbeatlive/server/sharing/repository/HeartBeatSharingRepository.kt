package com.munoon.heartbeatlive.server.sharing.repository

import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.subscription.account.limit.SimpleAccountSubscriptionLimitRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

interface HeartBeatSharingRepository : CoroutineSortingRepository<HeartBeatSharing, String> {

    suspend fun existsByPublicCode(publicCode: String): Boolean

    suspend fun findByPublicCode(publicCode: String): HeartBeatSharing?

    suspend fun deleteByIdAndUserId(id: String, userId: String): Int

    suspend fun deleteHeartBeatSharingById(id: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<HeartBeatSharing>

    suspend fun countAllByUserId(userId: String): Int

    suspend fun countAllByUserIdAndLockedTrue(userId: String): Int

    suspend fun deleteAllByUserId(userId: String)
}

@Repository
class HeartBeatSharingLimitRepository(
    mongoTemplate: ReactiveMongoTemplate,
    private val heartBeatSharingRepository: HeartBeatSharingRepository
) : SimpleAccountSubscriptionLimitRepository(
    mongoTemplate,
    collectionName = HeartBeatSharing.COLLECTION_NAME,
    userIdFieldName = "userId",
    lockedFieldName = "locked"
) {
    override suspend fun countAllByUserId(userId: String) =
        heartBeatSharingRepository.countAllByUserId(userId)

    override suspend fun countAllByUserIdAndLockedTrue(userId: String) =
        heartBeatSharingRepository.countAllByUserIdAndLockedTrue(userId)
}