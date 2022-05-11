package com.munoon.heartbeatlive.server.sharing.service

import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.sharing.*
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.create
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.repository.HeartBeatSharingRepository
import com.munoon.heartbeatlive.server.subscription.UserSubscriptionPlan
import kotlinx.coroutines.reactive.asFlow
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class HeartBeatSharingService(
    private val repository: HeartBeatSharingRepository,
    private val subscriptionProperties: SubscriptionProperties
) {
    suspend fun createSharing(
        input: GraphqlCreateSharingCodeInput,
        userId: String,
        userSubscriptionPlan: UserSubscriptionPlan
    ): HeartBeatSharing {
        val totalUserSharingCodeCount = repository.countAllByUserId(userId)
        if (totalUserSharingCodeCount >= subscriptionProperties[userSubscriptionPlan].maxSharingCodesLimit) {
            throw HeartBeatSharingLimitExceededException(subscriptionProperties[userSubscriptionPlan].maxSharingCodesLimit)
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

    suspend fun updateSharingCodeExpireTime(id: String, expiredAt: Instant?, validateUserId: String?): HeartBeatSharing {
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
}