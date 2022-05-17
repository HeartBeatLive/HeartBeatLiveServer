package com.munoon.heartbeatlive.server.ban.model

import java.time.Instant

data class GraphqlBanInfo(
    val id: String,
    val banTime: Instant,

    // for mappings only
    val userId: String,
    val bannedUserId: String
)
