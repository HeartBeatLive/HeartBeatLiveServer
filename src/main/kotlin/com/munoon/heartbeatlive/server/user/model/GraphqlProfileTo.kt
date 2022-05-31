package com.munoon.heartbeatlive.server.user.model

import com.munoon.heartbeatlive.server.user.UserRole
import java.time.Instant

data class GraphqlProfileTo(
    val id: String,
    val displayName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val roles: Set<UserRole>,
    val lastHeartRateInfoReceiveTime: Instant? // for mapping only
)
