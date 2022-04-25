package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.model.PublicProfileTo

fun User.asPublicProfile() = PublicProfileTo(
    id = id,
    displayName = displayName,
    email = email,
    emailVerified = emailVerified
)