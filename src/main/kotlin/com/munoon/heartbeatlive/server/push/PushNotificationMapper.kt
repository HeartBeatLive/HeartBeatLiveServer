package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotification
import org.springframework.context.MessageSource
import java.util.*

object PushNotificationMapper {
    fun PushNotification.asGraphQL() = GraphqlPushNotification(
        id = id,
        created = created,
        data = when (data) {
            is PushNotification.Data.HeartRateMatchData -> GraphqlHeartRateMatchPushNotificationData(
                data.heartRate, data.matchWithUserId)
            is PushNotification.Data.HighHeartRateData -> GraphqlHighHeartRatePushNotificationData(
                data.heartRate, data.heartRateOwnerUserId)
            is PushNotification.Data.LowHeartRateData -> GraphqlLowHeartRatePushNotificationData(
                data.heartRate, data.heartRateOwnerUserId)
            is PushNotification.Data.BanData -> GraphqlBannedPushNotificationData(data.bannedByUserId)
            is PushNotification.Data.HighOwnHeartRateData -> GraphqlHighOwnHeartRatePushNotificationData(data.heartRate)
            is PushNotification.Data.LowOwnHeartRateData -> GraphqlLowOwnHeartRatePushNotificationData(data.heartRate)
            is PushNotification.Data.NewSubscriberData -> GraphqlNewSubscriberPushNotificationData(data.subscriptionId)
        }
    )

    // locale is one of PushNotificationLocale
    fun MessageSource.getMessageText(message: PushNotificationMessage.Message, locale: Locale) =
        getMessage(message.code, message.arguments.toTypedArray(), when (locale) {
            PushNotificationLocale.EN -> Locale.ROOT
            PushNotificationLocale.RU -> Locale("ru")
            else -> throw IllegalArgumentException("Unknown locale: $locale")
        })
}