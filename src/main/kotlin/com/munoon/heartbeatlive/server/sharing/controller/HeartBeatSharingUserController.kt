package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.sharing.model.GraphqlPublicSharingCode
import com.munoon.heartbeatlive.server.sharing.model.GraphqlSharingCode
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture

@Controller
class HeartBeatSharingUserController {
    @SchemaMapping(typeName = "SharingCode", field = "user")
    fun mapSharingCodeUser(
        sharingCode: GraphqlSharingCode,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlProfileTo> {
        return userByIdLoader.load(sharingCode.userId).thenApply { it.asGraphqlProfile() }
    }

    @SchemaMapping(typeName = "PublicSharingCode", field = "user")
    fun mapPublicSharingCodeUser(
        sharingCode: GraphqlPublicSharingCode,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlProfileTo> {
        return userByIdLoader.load(sharingCode.userId).thenApply { it.asGraphqlProfile() }
    }
}