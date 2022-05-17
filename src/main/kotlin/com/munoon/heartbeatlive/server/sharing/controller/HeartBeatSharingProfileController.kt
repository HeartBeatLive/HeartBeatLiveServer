package com.munoon.heartbeatlive.server.sharing.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserSubscription
import com.munoon.heartbeatlive.server.common.CommonUtils.asGraphqlPage
import com.munoon.heartbeatlive.server.common.GraphqlPageResult
import com.munoon.heartbeatlive.server.config.properties.UserSharingProperties
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.asGraphQL
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByIdException
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.model.GraphqlSharingCode
import com.munoon.heartbeatlive.server.sharing.model.SharingCodeSorting
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.Instant
import javax.validation.Valid
import javax.validation.constraints.Future
import javax.validation.constraints.Max
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Controller
@PreAuthorize("isAuthenticated()")
class HeartBeatSharingProfileController(
    private val service: HeartBeatSharingService,
    private val userSharingProperties: UserSharingProperties
) {
    private val logger = LoggerFactory.getLogger(HeartBeatSharingProfileController::class.java)

    @QueryMapping
    suspend fun getSharingCodeById(@Argument id: String): GraphqlSharingCode {
        logger.info("User '${authUserId()}' request sharing code with id '$id'")
        val sharingCode = service.getSharingCodeById(id)

        if (sharingCode.userId != authUserId()) {
            throw HeartBeatSharingNotFoundByIdException(id)
        }

        return sharingCode.asGraphQL(userSharingProperties)
    }

    @MutationMapping
    suspend fun createSharingCode(@Argument @Valid data: GraphqlCreateSharingCodeInput?): GraphqlSharingCode {
        logger.info("User '${authUserId()}' create sharing code: $data")
        val createSharingCodeInput = data ?: GraphqlCreateSharingCodeInput(expiredAt = null)
        return service.createSharing(createSharingCodeInput, authUserId(), authUserSubscription())
            .asGraphQL(userSharingProperties)
    }

    @MutationMapping
    suspend fun updateSharingCodeExpireTime(
        @Argument id: String,
        @Argument @Future expiredAt: Instant?
    ): GraphqlSharingCode {
        logger.info("User '${authUserId()}' update sharing code '$id' expire time to '$expiredAt'")
        return service.updateSharingCodeExpireTime(id, expiredAt, authUserId()).asGraphQL(userSharingProperties)
    }

    @MutationMapping
    suspend fun deleteSharingCodeById(@Argument id: String): Boolean {
        logger.info("User '${authUserId()}' delete sharing code with id '$id'")
        service.deleteSharingCodeById(id, authUserId())
        return true
    }

    @SchemaMapping(typeName = "Profile", field = "sharingCodes")
    suspend fun getProfileSharingCode(
        @Argument @PositiveOrZero page: Int,
        @Argument @Positive @Max(20) size: Int,
        @Argument sort: SharingCodeSorting?
    ): GraphqlPageResult<GraphqlSharingCode> {
        logger.info("User '${authUserId()}' request his sharing codes (page = $page, size = $size, sort = $sort)")

        val sorting = when (sort) {
            SharingCodeSorting.CREATED_ASC -> Sort.by(Sort.Direction.ASC, "created")
            SharingCodeSorting.CREATED_DESC -> Sort.by(Sort.Direction.DESC, "created")
            null -> Sort.unsorted()
        }
        val pageRequest = PageRequest.of(page, size, sorting)

        return service.getSharingCodesByUserId(pageRequest, authUserId())
            .map { it.asGraphQL(userSharingProperties) }
            .asGraphqlPage(page, size)
    }
}