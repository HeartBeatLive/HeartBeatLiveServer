package com.munoon.heartbeatlive.server.ban

sealed interface UserBanEvents {
    data class UserBannedEvent(val userId: String, val bannedByUserId: String) : UserBanEvents
}