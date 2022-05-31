package com.munoon.heartbeatlive.server.heartrate.repository

import com.munoon.heartbeatlive.server.heartrate.HeartRateSubscriber
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = false)
interface HeartRateSubscriberRepository : CoroutineSortingRepository<HeartRateSubscriber, String> {
    suspend fun countAllByUserId(userId: String): Int
}