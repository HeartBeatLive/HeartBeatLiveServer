package com.munoon.heartbeatlive.server.ban.repository

import com.munoon.heartbeatlive.server.ban.UserBan
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

interface UserBanRepository : CoroutineSortingRepository<UserBan, String> {
    suspend fun existsByUserIdAndBannedUserId(userId: String, bannedUserId: String): Boolean

    suspend fun findByUserIdAndBannedUserId(userId: String, bannedUserId: String): UserBan?

    suspend fun deleteUserBanById(id: String): Int

    suspend fun deleteUserBanByIdAndUserId(id: String, userId: String): Int

    fun findAllByUserId(userId: String, pageable: Pageable): Flow<UserBan>

    suspend fun countAllByUserId(userId: String): Int

    @Query("{ \$or: [ { userId: ?0 }, { bannedUserId: ?0 } ] }", delete = true)
    suspend fun deleteAllByBannedUserIdOrUserId(userId: String)
}