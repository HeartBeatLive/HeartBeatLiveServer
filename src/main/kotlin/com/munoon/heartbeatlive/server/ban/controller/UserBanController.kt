package com.munoon.heartbeatlive.server.ban.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.ban.UserBanMapper.asGraphql
import com.munoon.heartbeatlive.server.ban.model.BanInfoSorting
import com.munoon.heartbeatlive.server.ban.model.GraphqlBanInfo
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.common.CommonUtils.asGraphqlPage
import com.munoon.heartbeatlive.server.common.GraphqlPageResult
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import javax.validation.constraints.Max
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Controller
@PreAuthorize("isAuthenticated()")
class UserBanController(
    private val service: UserBanService,
    private val subscriptionService: SubscriptionService
) {
    private val logger = LoggerFactory.getLogger(UserBanController::class.java)

    @MutationMapping
    suspend fun banUserBySubscriptionId(@Argument subscriptionId: String): GraphqlBanInfo {
        logger.info("User '${authUserId()}' ban user by subscription id '$subscriptionId'")

        val subscription = subscriptionService.getSubscriptionById(subscriptionId)
        if (subscription.userId != authUserId()) {
            throw SubscriptionNotFoundByIdException(subscriptionId)
        }

        return service.banUser(authUserId(), subscription.subscriberUserId).asGraphql()
    }

    @MutationMapping
    suspend fun unbanUserById(@Argument id: String): Boolean {
        logger.info("User '${authUserId()}' unban user by ban id '$id'")
        service.unbanUser(id, validateUserId = authUserId())
        return true
    }

    @QueryMapping
    suspend fun getBannedUsers(
        @Argument @PositiveOrZero page: Int,
        @Argument @Positive @Max(20) size: Int,
        @Argument sort: BanInfoSorting?
    ): GraphqlPageResult<GraphqlBanInfo> {
        logger.info("User '${authUserId()}' requested banned users (page = $page, size = $size, sort = $sort)")

        val sorting = when (sort) {
            BanInfoSorting.CREATED_ASC -> Sort.by(Sort.Direction.ASC, "created")
            BanInfoSorting.CREATED_DESC -> Sort.by(Sort.Direction.DESC, "created")
            null -> Sort.unsorted()
        }

        val pageRequest = PageRequest.of(page, size, sorting)
        return service.getBannedUsers(authUserId(), pageRequest)
            .map { it.asGraphql() }
            .asGraphqlPage(page, size)
    }
}