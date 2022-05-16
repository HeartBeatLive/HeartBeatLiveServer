package com.munoon.heartbeatlive.server.ban

import com.munoon.heartbeatlive.server.ban.model.GraphqlBanInfo

object UserBanMapper {
    fun UserBan.asGraphql() = GraphqlBanInfo(
        id = id!!,
        banTime = created,
        userId = userId,
        bannedUserId = bannedUserId
    )
}