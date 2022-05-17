package com.munoon.heartbeatlive.server.sharing.model

import java.time.Instant

data class GraphqlPublicSharingCode(
    val publicCode: String,
    val sharingUrl: String,
    val created: Instant,
    val expiredAt: Instant?,
    val userId: String // for user mapping only
)
