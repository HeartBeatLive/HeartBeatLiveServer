package com.munoon.heartbeatlive.server.sharing

import com.munoon.heartbeatlive.server.config.properties.UserSharingProperties
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.model.GraphqlPublicSharingCode
import com.munoon.heartbeatlive.server.sharing.model.GraphqlSharingCode

object HeartBeatSharingMapper {
    fun GraphqlCreateSharingCodeInput.create(userId: String) = HeartBeatSharing(
        id = null,
        publicCode = HeartBeatSharingUtils.generatePublicCode(),
        userId = userId,
        expiredAt = expiredAt
    )

    fun HeartBeatSharing.asGraphQL(properties: UserSharingProperties) = GraphqlSharingCode(
        id = id!!,
        publicCode = publicCode,
        sharingUrl = buildSharingUrl(properties),
        created = created,
        expiredAt = expiredAt,
        locked = locked,
        userId = userId
    )

    fun HeartBeatSharing.asPublicGraphQL(properties: UserSharingProperties) = GraphqlPublicSharingCode(
        publicCode = publicCode,
        sharingUrl = buildSharingUrl(properties),
        created = created,
        expiredAt = expiredAt,
        userId = userId
    )

    private fun HeartBeatSharing.buildSharingUrl(properties: UserSharingProperties) =
        properties.sharingUrlTemplate.format(publicCode)
}