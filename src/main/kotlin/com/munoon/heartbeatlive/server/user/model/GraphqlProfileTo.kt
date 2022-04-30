package com.munoon.heartbeatlive.server.user.model

import com.munoon.heartbeatlive.server.user.UserRole

data class GraphqlProfileTo(
    val id: String,
    val displayName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val roles: Set<UserRole>
)
