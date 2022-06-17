package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlSubscriptionUserProfileTo

object UserMapper {
    fun User.asGraphqlProfile() = GraphqlProfileTo(
        id = id,
        displayName = displayName,
        email = email,
        emailVerified = emailVerified,
        roles = roles,
        heartRates = heartRates
    )

    fun User.asGraphqlPublicProfile() = GraphqlPublicProfileTo(
        displayName = displayName
    )

    fun User.asGraphqlSubscriptionUserProfile() = GraphqlSubscriptionUserProfileTo(
        displayName = displayName,
        heartRates = heartRates
    )

    fun GraphqlFirebaseCreateUserInput.asNewUser() = User(
        id = id,
        displayName = null,
        email = email?.lowercase(),
        emailVerified = emailVerified
    )
}