package com.munoon.heartbeatlive.server.push.repository

import com.munoon.heartbeatlive.server.push.PushNotification
import org.springframework.data.repository.kotlin.CoroutineSortingRepository
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
interface PushNotificationRepository : CoroutineSortingRepository<PushNotification, String>