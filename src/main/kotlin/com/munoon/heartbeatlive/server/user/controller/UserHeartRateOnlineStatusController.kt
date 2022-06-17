package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.user.UserUtils
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlSubscriptionUserProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlUserHeartRateOnlineStatus
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
class UserHeartRateOnlineStatusController(
    private val heartRateStreamProperties: HeartRateStreamProperties
) {
    @SchemaMapping(typeName = "Profile", field = "heartRateOnlineStatus")
    fun getProfileHeartRateOnlineStatus(profile: GraphqlProfileTo): GraphqlUserHeartRateOnlineStatus {
        return UserUtils.getHeartRateOnlineStatus(profile.heartRates, heartRateStreamProperties)
    }

    @SchemaMapping(typeName = "SubscriptionUserProfile", field = "heartRateOnlineStatus")
    fun getSubscriptionUserProfileHeartRateOnlineStatus(
        profile: GraphqlSubscriptionUserProfileTo
    ): GraphqlUserHeartRateOnlineStatus {
        return UserUtils.getHeartRateOnlineStatus(profile.heartRates, heartRateStreamProperties)
    }
}