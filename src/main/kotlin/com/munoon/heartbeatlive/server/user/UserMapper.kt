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
        lastHeartRateInfoReceiveTime = lastHeartRateInfoReceiveTime
    )

    fun User.asGraphqlPublicProfile() = GraphqlPublicProfileTo(
        displayName = displayName
    )

    fun User.asGraphqlSubscriptionUserProfile() = GraphqlSubscriptionUserProfileTo(
        displayName = displayName,
        lastHeartRateInfoReceiveTime = lastHeartRateInfoReceiveTime
    )

    fun GraphqlFirebaseCreateUserInput.asNewUser() = User(
        id = id,
        displayName = null,
        email = email?.lowercase(),
        emailVerified = emailVerified
    )
}