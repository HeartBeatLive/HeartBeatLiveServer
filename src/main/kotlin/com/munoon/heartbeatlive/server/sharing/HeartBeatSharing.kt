package com.munoon.heartbeatlive.server.sharing

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("heartBeatSharing")
data class HeartBeatSharing(
    @Id
    val id: String?,

    @Indexed(unique = true)
    val publicCode: String,

    val userId: String,

    val created: Instant = Instant.now(),

    val expiredAt: Instant?
) {
    companion object {
        const val UNIQUE_PUBLIC_CODE_INDEX = "heartBeatSharing_unique_publicCode_index"
    }
}