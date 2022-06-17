package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.config.GraphqlTypeResolver
import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationData
import graphql.TypeResolutionEnvironment
import graphql.schema.GraphQLObjectType
import org.springframework.stereotype.Component

@Component
class GraphqlPushNotificationDataTypeResolver : GraphqlTypeResolver {
    override val typeName: String = "PushNotificationData"

    override fun getType(env: TypeResolutionEnvironment): GraphQLObjectType {
        val schemaTypeName = when (env.getObject<GraphqlPushNotificationData>()) {
            is GraphqlBannedPushNotificationData -> "BannedPushNotificationData"
            is GraphqlHeartRateMatchPushNotificationData -> "HeartRateMatchPushNotificationData"
            is GraphqlHighHeartRatePushNotificationData -> "HighHeartRatePushNotificationData"
            is GraphqlHighOwnHeartRatePushNotificationData -> "HighOwnHeartRatePushNotificationData"
            is GraphqlLowHeartRatePushNotificationData -> "LowHeartRatePushNotificationData"
            is GraphqlLowOwnHeartRatePushNotificationData -> "LowOwnHeartRatePushNotificationData"
            is GraphqlNewSubscriberPushNotificationData -> "NewSubscriberPushNotificationData"
        }
        return env.schema.getObjectType(schemaTypeName)
    }
}