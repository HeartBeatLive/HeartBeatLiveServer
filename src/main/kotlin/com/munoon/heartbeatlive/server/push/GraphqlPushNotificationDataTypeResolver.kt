package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.config.AbstractGraphqlTypeNameResolver
import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationData
import org.springframework.stereotype.Component

@Component
class GraphqlPushNotificationDataTypeResolver
    : AbstractGraphqlTypeNameResolver<GraphqlPushNotificationData>("PushNotificationData") {
    override fun getTypeName(obj: GraphqlPushNotificationData) = when (obj) {
        is GraphqlBannedPushNotificationData -> "BannedPushNotificationData"
        is GraphqlHeartRateMatchPushNotificationData -> "HeartRateMatchPushNotificationData"
        is GraphqlHighHeartRatePushNotificationData -> "HighHeartRatePushNotificationData"
        is GraphqlHighOwnHeartRatePushNotificationData -> "HighOwnHeartRatePushNotificationData"
        is GraphqlLowHeartRatePushNotificationData -> "LowHeartRatePushNotificationData"
        is GraphqlLowOwnHeartRatePushNotificationData -> "LowOwnHeartRatePushNotificationData"
        is GraphqlNewSubscriberPushNotificationData -> "NewSubscriberPushNotificationData"
    }
}