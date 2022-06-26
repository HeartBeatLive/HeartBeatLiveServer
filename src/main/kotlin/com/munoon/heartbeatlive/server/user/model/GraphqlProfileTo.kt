package com.munoon.heartbeatlive.server.user.model

import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.User.Role

data class GraphqlProfileTo(
    val id: String,
    val displayName: String?,
    val email: String?,
    val emailVerified: Boolean,
    val roles: Set<Role>,
    val heartRates: List<User.HeartRate> // for mapping only
)
