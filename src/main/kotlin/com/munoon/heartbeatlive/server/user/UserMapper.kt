package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo

fun User.asGraphqlProfile() = GraphqlProfileTo(
    id = id,
    displayName = displayName,
    email = email,
    emailVerified = emailVerified,
    roles = roles
)