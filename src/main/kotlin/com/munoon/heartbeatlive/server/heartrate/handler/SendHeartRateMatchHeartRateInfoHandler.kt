package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.common.FilterExpression
import com.munoon.heartbeatlive.server.common.GetFieldExpression
import com.munoon.heartbeatlive.server.common.LookupAggregation
import com.munoon.heartbeatlive.server.common.MapExpression
import com.munoon.heartbeatlive.server.common.MongodbUtils.getList
import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.heartrate.HeartRateUtils.mapHeartRateToInteger
import com.munoon.heartbeatlive.server.push.HeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.bson.Document
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.ArrayElemAt
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.IndexOfArray
import org.springframework.data.mongodb.core.aggregation.BooleanOperators.And.and
import org.springframework.data.mongodb.core.aggregation.BooleanOperators.Or.or
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Eq
import org.springframework.data.mongodb.core.aggregation.ComparisonOperators.Gte
import org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToDate.toDate
import org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToInt.toInt
import org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToString.toString
import org.springframework.data.mongodb.core.aggregation.EvaluationOperators.EvaluationOperatorFactory.Expr
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*
import kotlin.reflect.jvm.jvmName

@Component
class SendHeartRateMatchHeartRateInfoHandler(
    private val mongoTemplate: ReactiveMongoTemplate,
    private val pushNotificationService: PushNotificationService,
    private val heartRateStreamProperties: HeartRateStreamProperties,
    private val subscriptionProperties: SubscriptionProperties
) : HeartRateInfoHandler {
    override suspend fun handleHeartRateInfo(userId: String, heartRate: Float) {
        val aggregation = buildAggregation(userId, heartRate)
        val notifications = mongoTemplate.aggregate(aggregation, "subscription", Document::class.java)
            .asFlow()
            .toList(arrayListOf())
            .flatMap { getNotificationsFromDocument(it, heartRate) }

        pushNotificationService.sendNotifications(notifications)
    }

    private fun getNotificationsFromDocument(document: Document, heartRate: Float): List<PushNotificationData> {
        val result = mutableListOf<PushNotificationData>()

        val usersThatReceivedNotification = document.getList<Document>("usersThatReceivedNotification")
            .map { it.getString("_id") }

        val userA = document.getList<Document>("userAInfo").first()
        val userB = document.getList<Document>("userBInfo").first()

        val userAId = userA.getString("_id")
        val userBId = userB.getString("_id")

        val userASubscription = document.getList<Document>("userB").first()

        val userBWantToReceiveNotificationsOfUserA = document.getBoolean("receiveHeartRateMatchNotifications")
        val userAWantToReceiveNotificationsOfUserB =
            userASubscription.getBoolean("receiveHeartRateMatchNotifications")

        val sendHeartRateToUserA = userAWantToReceiveNotificationsOfUserB
                && !usersThatReceivedNotification.contains(userAId)
                && checkIsAccountSubscriptionAllow(userA.get("subscription", Document::class.java))
                && checkSubscriptionIsUnlocked(userASubscription)

        val sendHeartRateToUserB = userBWantToReceiveNotificationsOfUserA
                && !usersThatReceivedNotification.contains(userBId)
                && checkIsAccountSubscriptionAllow(userB.get("subscription", Document::class.java))
                && checkSubscriptionIsUnlocked(document)

        if (sendHeartRateToUserA) {
            result += HeartRateMatchPushNotificationData(
                heartRate = heartRate,
                userId = userAId,
                matchWithUserId = userBId,
                matchWithUserDisplayName = userB.getString("displayName")
            )
        }

        if (sendHeartRateToUserB) {
            result += HeartRateMatchPushNotificationData(
                heartRate = heartRate,
                userId = userBId,
                matchWithUserId = userAId,
                matchWithUserDisplayName = userA.getString("displayName")
            )
        }

        return result
    }

    private fun buildAggregation(userId: String, heartRate: Float) = Aggregation.newAggregation(listOfNotNull(
        // {..Subscription}
        matchSubscriptionsByUserId(userId),

        // {..Subscription, userB = [{..Subscription}]}
        findTwoWaySubscriptions(userId),

        filterWithNotificationsOnAndUnlocked,

        // {..Subscription, userB = [{..Subscription}], userBInfo=[{..User}]}
        getUserBInfo,

        filterSubscriptionsWithMatchedHeartRates(
            heartRate,
            checkHeartRatesFrom = Instant.now() - heartRateMatchPush.includeHeartRatesForDuration
        ),

        // {
        //   ..Subscription, userB = [{..Subscription}], userBInfo=[{..User}],
        //   usersThatReceivedNotification=[repeated {_id: `userId`}]
        // }
        getUsersReceivedHeartRatePushNotification(
            getNotificationFrom = Instant.now() - heartRateMatchPush.sendPushTimeoutDuration
        ),

        filterWhenBotUsersReceivedNotification,

        // {
        //   ..Subscription, userB = [{..Subscription}], userBInfo=[{..User}],
        //   usersThatReceivedNotification=[repeated {_id: `userId`}], userAInfo=[{..User}]
        // }
        getUserAInfo,

        filterIfBothAccountSubscriptionUnsupported
    ))

    private fun matchSubscriptionsByUserId(userId: String) =
        Aggregation.match(where("userId").isEqualTo(userId))

    private fun findTwoWaySubscriptions(userId: String) = LookupAggregation(
        from = "subscription",
        localField = "subscriberUserId",
        foreignField = "userId",
        asField = "userB",
        pipeline = Aggregation.newAggregation(
            Aggregation.match(where("subscriberUserId").isEqualTo(userId)),
            Aggregation.limit(1)
        )
    )

    private fun filterSubscriptionsWithMatchedHeartRates(
        heartRate: Float,
        checkHeartRatesFrom: Instant,
    ): AggregationOperation {
        val userBHeartRatesInfo = GetFieldExpression(
            field = "heartRates",
            input = ArrayElemAt.arrayOf("\$userBInfo").elementAt(0)
        )

        // filter heart rates from specific time
        val filteredHeartRates = FilterExpression(
            input = userBHeartRatesInfo,
            asField = "heartRate",
            cond = Gte.valueOf("\$\$heartRate.time").greaterThanEqualTo(toDate(checkHeartRatesFrom))
        )

        // map {heartRate: 10, time: "12:00"} -> 10 (extract `heartRate`)
        val parsedHeartRates = MapExpression(
            input = filteredHeartRates,
            asField = "heartRate",
            expr = "\$\$heartRate.heartRate"
        )

        // check if array contains specified heart rate
        val indexOfHeartRate = IndexOfArray.arrayOf(parsedHeartRates).indexOf(toInt(mapHeartRateToInteger(heartRate)))
        val heartRatesMatch = Gte.valueOf(indexOfHeartRate).greaterThanEqualTo(toInt(0))

        return Aggregation.match(Expr.valueOf(heartRatesMatch))
    }

    private fun getUsersReceivedHeartRatePushNotification(getNotificationFrom: Instant) = LookupAggregation(
        from = "pushNotification",
        asField = "usersThatReceivedNotification",
        let = mapOf("userA" to "\$userId", "userB" to "\$subscriberUserId"),
        pipeline = Aggregation.newAggregation(
            Aggregation.match(Expr.valueOf(and(
                Eq.valueOf("\$data._class").equalTo(HEART_RATE_MATCH_NOTIFICATION_DATA_CLASS_NAME),
                or(
                    and(
                        Eq.valueOf("\$userId").equalTo("\$\$userA"),
                        Eq.valueOf("\$data.matchWithUserId").equalTo("\$\$userB")
                    ),
                    and(
                        Eq.valueOf("\$userId").equalTo("\$\$userB"),
                        Eq.valueOf("\$data.matchWithUserId").equalTo("\$\$userA")
                    )
                ),
                Gte.valueOf("\$created").greaterThanEqualTo(toDate(getNotificationFrom)),
            ))),
            Aggregation.group("\$userId")
        )
    )

    private val heartRateMatchPush: HeartRateStreamProperties.HeartRateMatchPushSettings
        get() = heartRateStreamProperties.heartRateMatchPush

    private val filterIfBothAccountSubscriptionUnsupported: MatchOperation?
        get() {
            if (subscriptionProperties[UserSubscriptionPlan.FREE].limits.receiveHeartRateMatchNotification) {
                return null
            }

            val subscriptionPlansThatCouldReceiveNotification = subscriptionProperties.subscription.keys
                .filter { subscriptionProperties[it].limits.receiveHeartRateMatchNotification }

            return Aggregation.match(
                or(
                    and(
                        mapOf("userAInfo.0.subscription.plan" to
                                mapOf("\$in" to subscriptionPlansThatCouldReceiveNotification)),
                        mapOf("userAInfo.0.subscription.expiresAt" to
                                mapOf("\$gt" to Instant.now()))

                    ),
                    and(
                        mapOf("userBInfo.0.subscription.plan" to
                                mapOf("\$in" to subscriptionPlansThatCouldReceiveNotification)),
                        mapOf("userBInfo.0.subscription.expiresAt" to
                                mapOf("\$gt" to Instant.now()))
                    )
                )
            )
        }

    private fun checkIsAccountSubscriptionAllow(accountSubscription: Document?): Boolean {
        if (accountSubscription == null || accountSubscription.getDate("expiresAt") < Date()) {
            return subscriptionProperties[UserSubscriptionPlan.FREE].limits.receiveHeartRateMatchNotification
        }

        val userSubscriptionPlan = UserSubscriptionPlan.valueOf(accountSubscription.getString("plan"))
        return subscriptionProperties[userSubscriptionPlan].limits.receiveHeartRateMatchNotification
    }

    private companion object {
        val HEART_RATE_MATCH_NOTIFICATION_DATA_CLASS_NAME =
            toString(PushNotification.Data.HeartRateMatchData::class.jvmName)

        val getUserAInfo = LookupAggregation(
            from = "users",
            localField = "userId",
            foreignField = "_id",
            asField = "userAInfo"
        )

        val getUserBInfo = LookupAggregation(
            from = "users",
            localField = "userB.0.userId",
            foreignField = "_id",
            asField = "userBInfo"
        )

        val filterWithNotificationsOnAndUnlocked = Aggregation.match(
            and(
                or(
                    and(
                        mapOf("lock.byPublisher" to false),
                        mapOf("lock.bySubscriber" to false)
                    ),
                    and(
                        mapOf("userB.lock.byPublisher" to false),
                        mapOf("userB.lock.bySubscriber" to false)
                    )
                ),
                or(
                    mapOf("receiveHeartRateMatchNotifications" to true),
                    mapOf("userB.receiveHeartRateMatchNotifications" to true)
                )
            )
        )

        val filterWhenBotUsersReceivedNotification = Aggregation.match(
            Expr.valueOf(or(
                Eq.valueOf(
                    IndexOfArray.arrayOf("\$usersThatReceivedNotification")
                        .indexOf(mapOf("_id" to "\$userId"))
                ).equalTo(toInt(-1)),
                Eq.valueOf(
                    IndexOfArray.arrayOf("\$usersThatReceivedNotification")
                        .indexOf(mapOf("_id" to "\$subscriberUserId"))
                ).equalTo(toInt(-1))
            ))
        )

        fun checkSubscriptionIsUnlocked(subscription: Document): Boolean {
            val lock = subscription.get("lock", Document::class.java)
            return !lock.getBoolean("byPublisher") && !lock.getBoolean("bySubscriber")
        }
    }
}