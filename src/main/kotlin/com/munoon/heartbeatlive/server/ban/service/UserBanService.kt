package com.munoon.heartbeatlive.server.ban.service

import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.ban.UserBanNotFoundByIdException
import com.munoon.heartbeatlive.server.ban.repository.UserBanRepository
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.user.UserEvents
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class UserBanService(
    private val repository: UserBanRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    suspend fun checkUserBanned(userId: String, bannedByUserId: String): Boolean {
        return repository.existsByUserIdAndBannedUserId(bannedByUserId, userId)
    }

    suspend fun banUser(userId: String, userIdToBan: String): UserBan {
        return repository.findByUserIdAndBannedUserId(userId, userIdToBan)
            ?: repository.save(UserBan(
                userId = userId,
                bannedUserId = userIdToBan
            )).also { eventPublisher.publishEvent(UserBanEvents.UserBannedEvent(userIdToBan, userId)) }
    }

    suspend fun unbanUser(banId: String, validateUserId: String? = null) {
        val count = when (validateUserId) {
            null -> repository.deleteUserBanById(banId)
            else -> repository.deleteUserBanByIdAndUserId(banId, validateUserId)
        }

        if (count <= 0) {
            throw UserBanNotFoundByIdException(banId)
        }
    }

    suspend fun getBannedUsers(userId: String, pageable: Pageable): PageResult<UserBan> {
        return PageResult(
            data = repository.findAllByUserId(userId, pageable),
            totalItemsCount = repository.countAllByUserId(userId)
        )
    }

    @Async
    @EventListener
    fun handleUserDeletedEvent(event: UserEvents.UserDeletedEvent) {
        runBlocking { repository.deleteAllByBannedUserIdOrUserId(event.userId) }
    }
}