package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo

fun User.asGraphqlProfile() = GraphqlProfileTo(
    id = id,
    displayName = displayName,
    email = email,
    emailVerified = emailVerified,
    roles = roles
)

fun GraphqlFirebaseCreateUserInput.asNewUser() = User(
    id = id,
    displayName = null,
    email = email?.lowercase(),
    emailVerified = emailVerified
)