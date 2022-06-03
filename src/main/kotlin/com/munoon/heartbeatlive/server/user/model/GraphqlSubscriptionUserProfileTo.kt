package com.munoon.heartbeatlive.server.user.model

import java.time.Instant

data class GraphqlSubscriptionUserProfileTo(
    val displayName: String?,
    val lastHeartRateInfoReceiveTime: Instant? // for mapping only
)