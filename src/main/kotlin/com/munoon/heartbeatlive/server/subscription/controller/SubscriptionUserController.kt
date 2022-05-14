package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import kotlinx.coroutines.future.await
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller

@Controller
@SchemaMapping(typeName = "SubscriptionInfo")
class SubscriptionUserController {
    @SchemaMapping
    suspend fun user(subscription: GraphqlSubscriptionInfo, userByIdLoader: DataLoader<String, User>): GraphqlPublicProfileTo {
        return userByIdLoader.load(subscription.userId).await().asGraphqlPublicProfile()
    }

    @SchemaMapping
    suspend fun subscriber(subscription: GraphqlSubscriptionInfo, userByIdLoader: DataLoader<String, User>): GraphqlPublicProfileTo {
        return userByIdLoader.load(subscription.subscriberUserId).await().asGraphqlPublicProfile()
    }
}