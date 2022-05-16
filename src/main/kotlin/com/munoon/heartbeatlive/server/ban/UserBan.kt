package com.munoon.heartbeatlive.server.ban

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("userBan")
data class UserBan(
    @Id
    val id: String? = null,

    val userId: String,

    val bannedUserId: String,

    val created: Instant = Instant.now()
) {
    companion object {
        const val UNIQUE_USER_ID_AND_BANNED_USER_ID_INDEX = "userBan_unique_userId_bannedUserId_index"
    }
}
