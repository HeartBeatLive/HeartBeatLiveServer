package com.munoon.heartbeatlive.server.sharing.model

import java.time.Instant

data class GraphqlSharingCode(
    val id: String,
    val publicCode: String,
    val sharingUrl: String,
    val created: Instant,
    val expiredAt: Instant?,
    val locked: Boolean,
    val userId: String // for user mapping only
)
