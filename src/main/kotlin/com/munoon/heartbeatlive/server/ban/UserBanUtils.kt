package com.munoon.heartbeatlive.server.ban

import com.munoon.heartbeatlive.server.ban.service.UserBanService

object UserBanUtils {
    suspend fun UserBanService.validateUserBanned(userId: String, bannedByUserId: String) {
        if (checkUserBanned(userId, bannedByUserId)) {
            throw UserBanedByOtherUserException(userId, bannedByUserId)
        }
    }
}