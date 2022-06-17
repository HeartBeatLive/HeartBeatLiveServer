package com.munoon.heartbeatlive.server.user.model

import com.munoon.heartbeatlive.server.user.User

data class GraphqlSubscriptionUserProfileTo(
    val displayName: String?,
    val heartRates: List<User.HeartRate> // for mapping only
)