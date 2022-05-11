package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserIdOrAnonymous
import com.munoon.heartbeatlive.server.config.properties.UserSharingProperties
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.asPublicGraphQL
import com.munoon.heartbeatlive.server.sharing.model.GraphqlPublicSharingCode
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.service.UserService
import org.hibernate.validator.constraints.Length
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import javax.validation.constraints.NotNull

@Controller
class HeartBeatSharingPublicController(
    private val service: HeartBeatSharingService,
    private val userService: UserService,
    val userSharingProperties: UserSharingProperties
) {
    private val logger = LoggerFactory.getLogger(HeartBeatSharingPublicController::class.java)

    @QueryMapping
    suspend fun getSharingCodeByPublicCode(
        @Argument @NotNull @Length(min = 6, max = 6) publicCode: String
    ): GraphqlPublicSharingCode {
        logger.info("User '${authUserIdOrAnonymous()}' get sharing code with public code '$publicCode'")
        return service.getSharingCodeByPublicCode(publicCode).asPublicGraphQL(userSharingProperties)
    }

    @SchemaMapping(typeName = "PublicSharingCode", field = "user")
    suspend fun mapSharingCodeUser(sharingCode: GraphqlPublicSharingCode): GraphqlProfileTo {
        return userService.getUserById(sharingCode.userId).asGraphqlProfile()
    }
}