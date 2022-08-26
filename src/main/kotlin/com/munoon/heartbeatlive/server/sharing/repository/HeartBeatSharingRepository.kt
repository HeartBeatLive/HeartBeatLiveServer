package com.munoon.heartbeatlive.server.sharing.repository

import com.mongodb.client.result.UpdateResult
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.Document
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.BasicUpdate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
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
    suspend fun lockAllById(ids: Set<String>, lock: Boolean): UpdateResult

    suspend fun findIdsByUserId(userId: String, pageable: Pageable): Set<String>
}

@Repository
private class CustomHeartBeatSharingRepositoryImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : CustomHeartBeatSharingRepository {
    override suspend fun lockAllById(ids: Set<String>, lock: Boolean): UpdateResult {
        val query = Query.query(Criteria.where("_id").inValues(ids))
        val update = BasicUpdate.update("locked", lock)
        return mongoTemplate.updateMulti(query, update, HeartBeatSharing::class.java)
            .awaitSingle()
    }

    override suspend fun findIdsByUserId(userId: String, pageable: Pageable): Set<String> {
        val query = Query.query(Criteria.where("userId").isEqualTo(userId))
            .with(pageable)
            .apply { fields().include("_id") }

        return mongoTemplate.find(query, Document::class.java, HeartBeatSharing.COLLECTION_NAME)
            .asFlow().map { it.getObjectId("_id").toHexString() }.toSet(hashSetOf())
    }
}