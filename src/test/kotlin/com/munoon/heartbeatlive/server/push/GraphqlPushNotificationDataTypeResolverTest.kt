package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationData
import graphql.execution.TypeResolutionParameters
import graphql.schema.GraphQLSchema
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.freeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class GraphqlPushNotificationDataTypeResolverTest : FreeSpec({
    include(dataMapping(
        GraphqlBannedPushNotificationData(bannedByUserId = "user1"),
        "BannedPushNotificationData"
    ))

    include(dataMapping(
        GraphqlHeartRateMatchPushNotificationData(heartRate = 50f, matchWithUserId = "user1"),
        "HeartRateMatchPushNotificationData"
    ))

    include(dataMapping(
        GraphqlHighHeartRatePushNotificationData(heartRate = 50f, heartRateOwnerUserId = "user1"),
        "HighHeartRatePushNotificationData"
    ))

    include(dataMapping(
        GraphqlHighOwnHeartRatePushNotificationData(heartRate = 50f),
        "HighOwnHeartRatePushNotificationData"
    ))

    include(dataMapping(
        GraphqlLowHeartRatePushNotificationData(heartRate = 50f, heartRateOwnerUserId = "user1"),
        "LowHeartRatePushNotificationData"
    ))

    include(dataMapping(
        GraphqlLowOwnHeartRatePushNotificationData(heartRate = 50f),
        "LowOwnHeartRatePushNotificationData"
    ))

    include(dataMapping(
        GraphqlNewSubscriberPushNotificationData(subscriptionId = "subscriptionId"),
        "NewSubscriberPushNotificationData"
    ))
})

fun dataMapping(data: GraphqlPushNotificationData, graphqlSchemaName: String) = freeSpec {
    "should return '$graphqlSchemaName' type for ${data::class.simpleName} class" {
        val resolver = GraphqlPushNotificationDataTypeResolver()

        val schema = mockk<GraphQLSchema>() {
            every { getObjectType(any()) } returns mockk()
        }

        val typeResolutionEnvironment = TypeResolutionParameters.newParameters()
            .schema(schema)
            .value(data)
            .build()
        resolver.getType(typeResolutionEnvironment)

        verify(exactly = 1) { schema.getObjectType(graphqlSchemaName) }
    }
}
