package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.common.CommonUtils.asGraphqlPage
import com.munoon.heartbeatlive.server.common.GraphqlPageResult
import com.munoon.heartbeatlive.server.subscription.SubscriptionMapper.asGraphQL
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionSorting
import com.munoon.heartbeatlive.server.subscription.model.asSort
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import org.hibernate.validator.constraints.Length
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import javax.validation.constraints.Max
import javax.validation.constraints.NotNull
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Controller
@PreAuthorize("isAuthenticated()")
class SubscriptionController(private val service: SubscriptionService) {
    private val logger = LoggerFactory.getLogger(SubscriptionController::class.java)

    @MutationMapping
    suspend fun subscribeBySharingCode(
        @Argument @NotNull @Length(min = 6, max = 6) sharingCode: String
    ): GraphqlSubscriptionInfo {
        logger.info("User '${authUserId()}' create subscription using sharing code '$sharingCode'")
        return service.subscribeBySharingCode(sharingCode, authUserId()).asGraphQL()
    }

    @MutationMapping
    suspend fun unsubscribeFromUserById(@Argument @NotNull id: String): Boolean {
        logger.info("User '${authUserId()}' unsubscribe from subscription with id '$id'")
        service.unsubscribeFromUserById(id, authUserId())
        return true
    }

    @QueryMapping
    suspend fun getSubscriptionById(@Argument @NotNull id: String): GraphqlSubscriptionInfo {
        logger.info("User '${authUserId()}' request subscription with id '$id'")
        return service.getSubscriptionById(id)
            .also { subscription ->
                if (subscription.userId != authUserId() && subscription.subscriberUserId != authUserId()) {
                    throw SubscriptionNotFoundByIdException(id)
                }
            }
            .asGraphQL()
    }

    @SchemaMapping(typeName = "Profile", field = "subscribers")
    suspend fun getProfileSubscribers(
        @Argument @PositiveOrZero page: Int,
        @Argument @Positive @Max(20) size: Int,
        @Argument sort: GraphqlSubscriptionSorting?
    ): GraphqlPageResult<GraphqlSubscriptionInfo> {
        logger.info("User '${authUserId()}' requested his subscribers (page = $page, size = $size, sort = $sort)")
        return service.getSubscribers(authUserId(), PageRequest.of(page, size, sort.asSort()))
            .map { it.asGraphQL() }
            .asGraphqlPage(page, size)
    }

    @SchemaMapping(typeName = "Profile", field = "subscriptions")
    suspend fun getProfileSubscriptions(
        @Argument @PositiveOrZero page: Int,
        @Argument @Positive @Max(20) size: Int,
        @Argument sort: GraphqlSubscriptionSorting?
    ): GraphqlPageResult<GraphqlSubscriptionInfo> {
        logger.info("User '${authUserId()}' requested his subscriptions (page = $page, size = $size, sort = $sort)")
        return service.getSubscriptions(authUserId(), PageRequest.of(page, size, sort.asSort()))
            .map { it.asGraphQL() }
            .asGraphqlPage(page, size)
    }
}