package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.DataLoaders
import com.munoon.heartbeatlive.server.push.BanPushNotificationMessage
import com.munoon.heartbeatlive.server.push.FailedToRefundPushNotificationMessage
import com.munoon.heartbeatlive.server.push.HeartRateMatchPushNotificationMessage
import com.munoon.heartbeatlive.server.push.HighHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.HighOwnHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.LowHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.LowOwnHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.NewSubscriptionPushNotificationMessage
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationLocale.asPushNotificationLocale
import com.munoon.heartbeatlive.server.push.PushNotificationMapper.getMessageText
import com.munoon.heartbeatlive.server.push.PushNotificationMessage
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotification
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationInfo
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoaderRegistry
import org.springframework.context.MessageSource
import org.springframework.graphql.data.method.annotation.LocalContextValue
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.stereotype.Controller
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

@Controller
class PushNotificationInfoController(private val messageSource: MessageSource) {
    @SchemaMapping(typeName = "PushNotification", field = "info")
    fun getPushNotificationInfo(
        source: GraphqlPushNotification,
        @LocalContextValue pushNotifications: List<PushNotification>,
        locale: Locale?,
        env: DataFetchingEnvironment
    ): CompletableFuture<GraphqlPushNotificationInfo> {
        val pushNotification = pushNotifications.find { notification -> notification.id == source.id }
        val pushNotificationLocale = (locale ?: Locale.ENGLISH).asPushNotificationLocale()

        return loadPushNotificationMessage(pushNotification!!, env.dataLoaderRegistry)
            .thenApply { GraphqlPushNotificationInfo(
                title = messageSource.getMessageText(it.title, pushNotificationLocale),
                content = messageSource.getMessageText(it.content, pushNotificationLocale)
            ) }
    }

    private fun loadPushNotificationMessage(
        pushNotification: PushNotification,
        dataLoaderRegistry: DataLoaderRegistry
    ): CompletableFuture<out PushNotificationMessage> = when (pushNotification.data) {
        is PushNotification.Data.BanData -> DataLoaders.USER_BY_ID[dataLoaderRegistry]
            .load(pushNotification.data.bannedByUserId)
            .thenApply { user -> BanPushNotificationMessage(
                bannedByUserDisplayName = user?.displayName
            ) }

        is PushNotification.Data.HighHeartRateData -> DataLoaders.USER_BY_ID[dataLoaderRegistry]
            .load(pushNotification.data.heartRateOwnerUserId)
            .thenApply { user -> HighHeartRatePushNotificationMessage(
                heartRate = pushNotification.data.heartRate,
                heartRateOwnerUserDisplayName = user?.displayName
            ) }

        is PushNotification.Data.LowHeartRateData -> DataLoaders.USER_BY_ID[dataLoaderRegistry]
            .load(pushNotification.data.heartRateOwnerUserId)
            .thenApply { user -> LowHeartRatePushNotificationMessage(
                heartRate = pushNotification.data.heartRate,
                heartRateOwnerUserDisplayName = user?.displayName
            ) }

        is PushNotification.Data.HighOwnHeartRateData ->
            completedFuture(HighOwnHeartRatePushNotificationMessage(heartRate = pushNotification.data.heartRate))

        is PushNotification.Data.LowOwnHeartRateData ->
            completedFuture(LowOwnHeartRatePushNotificationMessage(heartRate = pushNotification.data.heartRate))

        is PushNotification.Data.HeartRateMatchData -> DataLoaders.USER_BY_ID[dataLoaderRegistry]
            .load(pushNotification.data.matchWithUserId)
            .thenApply { user -> HeartRateMatchPushNotificationMessage(
                heartRate = pushNotification.data.heartRate,
                matchWithUserDisplayName = user?.displayName
            ) }

        is PushNotification.Data.NewSubscriberData -> DataLoaders.USER_BY_ID[dataLoaderRegistry]
            .load(pushNotification.data.subscriberUserId)
            .thenApply { NewSubscriptionPushNotificationMessage(subscriberDisplayName = it?.displayName) }

        is PushNotification.Data.FailedToRefundData -> completedFuture(FailedToRefundPushNotificationMessage)
    }
}