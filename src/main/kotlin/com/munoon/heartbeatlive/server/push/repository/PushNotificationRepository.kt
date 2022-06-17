package com.munoon.heartbeatlive.server.push.repository

import com.munoon.heartbeatlive.server.push.PushNotification
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Transactional(readOnly = true)
interface PushNotificationRepository : CoroutineSortingRepository<PushNotification, String> {
    fun findAllByUserId(userId: String, pageable: Pageable): Flux<PushNotification>

    suspend fun countAllByUserId(userId: String): Int
}