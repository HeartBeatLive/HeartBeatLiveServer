package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlSubscriptionUserProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlSubscriptionUserProfileTo
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture

@Controller
@SchemaMapping(typeName = "SubscriptionInfo")
class SubscriptionUserController {
    @SchemaMapping
    fun user(
        subscription: GraphqlSubscriptionInfo,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlSubscriptionUserProfileTo> {
        return userByIdLoader.load(subscription.userId).thenApply { it.asGraphqlSubscriptionUserProfile() }
    }

    @SchemaMapping
    fun subscriber(
        subscription: GraphqlSubscriptionInfo,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(subscription.subscriberUserId).thenApply { it.asGraphqlPublicProfile() }
    }
}