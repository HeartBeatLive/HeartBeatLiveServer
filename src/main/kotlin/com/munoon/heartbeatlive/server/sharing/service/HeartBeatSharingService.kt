package com.munoon.heartbeatlive.server.sharing.service

import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingLimitExceededException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.create
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByIdException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByPublicCodeException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.repository.HeartBeatSharingRepository
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.limit.MaxSharingCodesAccountSubscriptionLimit
import com.munoon.heartbeatlive.server.user.UserEvents
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class HeartBeatSharingService(
    private val repository: HeartBeatSharingRepository,
    @Lazy private val maxSharingCodesAccountSubscriptionLimit: MaxSharingCodesAccountSubscriptionLimit
) {
    suspend fun createSharing(
        input: GraphqlCreateSharingCodeInput,
        userId: String,
        userSubscriptionPlan: UserSubscriptionPlan
    ): HeartBeatSharing {
        val totalUserSharingCodeCount = repository.countAllByUserId(userId)
        val limit = maxSharingCodesAccountSubscriptionLimit.getCurrentLimit(userSubscriptionPlan)
        if (totalUserSharingCodeCount >= limit) {
            throw HeartBeatSharingLimitExceededException(limit)
        }

        var sharingCode = input.create(userId)
        while (repository.existsByPublicCode(sharingCode.publicCode)) {
            sharingCode = sharingCode.copy(publicCode = HeartBeatSharingUtils.generatePublicCode())
        }

        return repository.save(sharingCode)
    }

    suspend fun getSharingCodeById(id: String): HeartBeatSharing {
        return repository.findById(id)
            ?: throw HeartBeatSharingNotFoundByIdException(id)
    }

    suspend fun getSharingCodeByPublicCode(publicCode: String): HeartBeatSharing {
        return repository.findByPublicCode(publicCode)
            ?: throw HeartBeatSharingNotFoundByPublicCodeException(publicCode)
    }

    suspend fun updateSharingCodeExpireTime(
        id: String,
        expiredAt: Instant?,
        validateUserId: String?
    ): HeartBeatSharing {
        val sharingCode = getSharingCodeById(id)

        if (validateUserId != null && validateUserId != sharingCode.userId) {
            throw HeartBeatSharingNotFoundByIdException(id)
        }

        return repository.save(sharingCode.copy(expiredAt = expiredAt))
    }

    suspend fun deleteSharingCodeById(id: String, validateUserId: String?) {
        val count = when (validateUserId) {
            null -> repository.deleteHeartBeatSharingById(id)
            else -> repository.deleteByIdAndUserId(id, validateUserId)
        }

        if (count <= 0) {
            throw HeartBeatSharingNotFoundByIdException(id)
        }
    }

    suspend fun getSharingCodesByUserId(pageable: Pageable, userId: String): PageResult<HeartBeatSharing> {
        return PageResult(
            data = repository.findAllByUserId(userId, pageable).asFlow(),
            totalItemsCount = repository.countAllByUserId(userId)
        )
    }

    suspend fun maintainUserLimit(userId: String, newLimit: Int) {
        val currentCount = repository.countAllByUserId(userId)

        // locking out of limits
        if (currentCount > newLimit) {
            val shouldBeLockedCount = currentCount - newLimit
            if (shouldBeLockedCount > 0) {
                val pageRequest = PageRequest.of(0, shouldBeLockedCount, Sort.by("created").ascending())
                val itemsToLock = repository.findIdsByUserId(userId, pageRequest)

                repository.lockAllById(itemsToLock, lock = true)
            }
        }

        // unlocking previously locked
        val currentlyLockedCount = repository.countAllByUserIdAndLockedTrue(userId)
        val canBeUnlockedCount = newLimit - (currentCount - currentlyLockedCount)
        if (canBeUnlockedCount > 0) {
            val pageRequest = PageRequest.of(0, canBeUnlockedCount, Sort.by("created").descending())
            val itemsToUnlock = repository.findIdsByUserId(userId, pageRequest)

            repository.lockAllById(itemsToUnlock, lock = false)
        }
    }

    @Async
    @EventListener
    fun handleUserDeletedEvent(event: UserEvents.UserDeletedEvent) {
        runBlocking { repository.deleteAllByUserId(event.userId) }
    }
}