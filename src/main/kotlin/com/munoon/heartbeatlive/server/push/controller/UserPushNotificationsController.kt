package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.common.CommonUtils.asGraphqlPage
import com.munoon.heartbeatlive.server.common.CommonUtils.graphqlContextOf
import com.munoon.heartbeatlive.server.common.GraphqlPageResult
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.push.PushNotificationMapper.asGraphQL
import com.munoon.heartbeatlive.server.push.PushNotificationNotFoundByIdException
import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotification
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationsSorting
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionMapper.asGraphQL
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscriptionInfo
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import graphql.execution.DataFetcherResult
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.dataloader.DataLoader
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.concurrent.CompletableFuture
import javax.validation.constraints.Max
import javax.validation.constraints.Positive
import javax.validation.constraints.PositiveOrZero

@Controller
class UserPushNotificationsController(
    private val pushNotificationService: PushNotificationService
) {
    private val logger = LoggerFactory.getLogger(UserPushNotificationsController::class.java)

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun getPushNotifications(
        @Argument @PositiveOrZero page: Int,
        @Argument @Positive @Max(20) size: Int,
        @Argument sort: GraphqlPushNotificationsSorting?
    ): DataFetcherResult<GraphqlPageResult<GraphqlPushNotification>>? {
        logger.info("User '${authUserId()}' requested his push notifications " +
                "(page = $page, size = $size, sort = $sort)")

        val pageable = PageRequest.of(page, size, when (sort) {
            GraphqlPushNotificationsSorting.CREATED_ASC -> Sort.by(Sort.Direction.ASC, "created")
            GraphqlPushNotificationsSorting.CREATED_DESC -> Sort.by(Sort.Direction.DESC, "created")
            null -> Sort.unsorted()
        })

        val pushNotificationPage = pushNotificationService.getPushNotificationsByUserId(authUserId(), pageable)
        val pushNotifications = pushNotificationPage.data.toList(arrayListOf())

        val pageResult = PageResult(
            data = pushNotifications.asFlow(),
            totalItemsCount = pushNotificationPage.totalItemsCount
        ).map { it.asGraphQL() }.asGraphqlPage(page, size)

        return DataFetcherResult.newResult<GraphqlPageResult<GraphqlPushNotification>>()
            .data(pageResult)
            .localContext(graphqlContextOf("pushNotifications" to pushNotifications))
            .build()
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun getPushNotificationById(@Argument id: String): DataFetcherResult<GraphqlPushNotification>? {
        logger.info("User '${authUserId()}' requested push notification with id '$id'")

        val pushNotification = pushNotificationService.getPushNotificationById(id)
        if (pushNotification.userId != authUserId()) {
            throw PushNotificationNotFoundByIdException(id)
        }

        return DataFetcherResult.newResult<GraphqlPushNotification>()
            .data(pushNotification.asGraphQL())
            .localContext(graphqlContextOf("pushNotifications" to listOf(pushNotification)))
            .build()
    }

    @SchemaMapping(typeName = "NewSubscriberPushNotificationData", field = "subscription")
    fun getNewSubscriberPushNotificationDataSubscription(
        data: GraphqlNewSubscriberPushNotificationData,
        subscriptionByIdLoader: DataLoader<String, Subscription>
    ): CompletableFuture<GraphqlSubscriptionInfo> {
        return subscriptionByIdLoader.load(data.subscriptionId).thenApply { it?.asGraphQL() }
    }

    @SchemaMapping(typeName = "BannedPushNotificationData", field = "bannedByUser")
    fun getBannedPushNotificationDataBannedByUser(
        data: GraphqlBannedPushNotificationData,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(data.bannedByUserId).thenApply { it?.asGraphqlPublicProfile() }
    }

    @SchemaMapping(typeName = "HighHeartRatePushNotificationData", field = "heartRateOwner")
    fun getHighHeartRatePushNotificationDataHeartRateOwner(
        data: GraphqlHighHeartRatePushNotificationData,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(data.heartRateOwnerUserId).thenApply { it?.asGraphqlPublicProfile() }
    }

    @SchemaMapping(typeName = "LowHeartRatePushNotificationData", field = "heartRateOwner")
    fun getLowHeartRatePushNotificationDataHeartRateOwner(
        data: GraphqlLowHeartRatePushNotificationData,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(data.heartRateOwnerUserId).thenApply { it?.asGraphqlPublicProfile() }
    }

    @SchemaMapping(typeName = "HeartRateMatchPushNotificationData", field = "matchWithUser")
    fun getHeartRateMatchPushNotificationDataMatchWithUser(
        data: GraphqlHeartRateMatchPushNotificationData,
        userByIdLoader: DataLoader<String, User>
    ): CompletableFuture<GraphqlPublicProfileTo> {
        return userByIdLoader.load(data.matchWithUserId).thenApply { it?.asGraphqlPublicProfile() }
    }
}