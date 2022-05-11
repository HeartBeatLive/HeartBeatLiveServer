package com.munoon.heartbeatlive.server.sharing.repository

import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import reactor.core.publisher.Flux

interface HeartBeatSharingRepository : CoroutineSortingRepository<HeartBeatSharing, String> {
    suspend fun existsByPublicCode(publicCode: String): Boolean

    suspend fun findByPublicCode(publicCode: String): HeartBeatSharing?

    suspend fun deleteByIdAndUserId(id: String, userId: String): Int

    suspend fun deleteHeartBeatSharingById(id: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flux<HeartBeatSharing>

    suspend fun countAllByUserId(userId: String): Int
}