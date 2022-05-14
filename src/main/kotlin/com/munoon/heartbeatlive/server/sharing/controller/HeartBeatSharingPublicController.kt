package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserIdOrAnonymous
import com.munoon.heartbeatlive.server.config.properties.UserSharingProperties
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.asPublicGraphQL
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils.checkExpired
import com.munoon.heartbeatlive.server.sharing.model.GraphqlPublicSharingCode
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.SubscriptionUtils.validateUserSubscribersCount
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import org.dataloader.DataLoader
import org.hibernate.validator.constraints.Length
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture
import javax.validation.constraints.NotNull

@Controller
class HeartBeatSharingPublicController(
    private val service: HeartBeatSharingService,
    private val userSharingProperties: UserSharingProperties,
    private val subscriptionService: SubscriptionService
) {
    private val logger = LoggerFactory.getLogger(HeartBeatSharingPublicController::class.java)

    @QueryMapping
    suspend fun getSharingCodeByPublicCode(
        @Argument @NotNull @Length(min = 6, max = 6) publicCode: String
    ): GraphqlPublicSharingCode {
        logger.info("User '${authUserIdOrAnonymous()}' get sharing code with public code '$publicCode'")
        return service.getSharingCodeByPublicCode(publicCode)
            .also { sharingCode ->
                // validate if someone still can subscribe to user
                sharingCode.checkExpired()
                subscriptionService.validateUserSubscribersCount(sharingCode.userId)
            }
            .asPublicGraphQL(userSharingProperties)
    }

    @SchemaMapping(typeName = "PublicSharingCode", field = "user")
    fun mapSharingCodeUser(sharingCode: GraphqlPublicSharingCode, userByIdLoader: DataLoader<String, User>): CompletableFuture<GraphqlProfileTo> {
        return userByIdLoader.load(sharingCode.userId).thenApply { it.asGraphqlProfile() }
    }
}