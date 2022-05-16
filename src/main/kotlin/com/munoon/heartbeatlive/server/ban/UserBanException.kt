package com.munoon.heartbeatlive.server.ban

data class UserBanedByOtherUserException(val userId: String, val bannedByUserId: String)
    : RuntimeException("User '$userId' is banned by user '$bannedByUserId'")

data class UserBanNotFoundByIdException(val id: String)
    : RuntimeException("User ban with id '$id' is not found!")