package com.munoon.heartbeatlive.server.ban.controller

import com.munoon.heartbeatlive.server.ban.model.GraphqlBanInfo
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import org.dataloader.DataLoader
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture

@Controller
@SchemaMapping(typeName = "BanInfo")
class UserBanUserInfoController {
    @SchemaMapping
    fun user(banInfo: GraphqlBanInfo, userByIdLoader: DataLoader<String, User>): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(banInfo.userId).thenApply { it.asGraphqlPublicProfile() }
    }

    @SchemaMapping
    fun bannedUser(banInfo: GraphqlBanInfo, userByIdLoader: DataLoader<String, User>): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(banInfo.bannedUserId).thenApply { it.asGraphqlPublicProfile() }
    }
}