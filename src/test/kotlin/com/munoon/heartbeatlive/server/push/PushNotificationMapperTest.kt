package com.munoon.heartbeatlive.server.push

import com.munoon.heartbeatlive.server.push.PushNotificationMapper.asGraphQL
import com.munoon.heartbeatlive.server.push.PushNotificationMapper.getMessageText
import com.munoon.heartbeatlive.server.push.model.GraphqlBannedPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHeartRateMatchPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlHighOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlLowOwnHeartRatePushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlNewSubscriberPushNotificationData
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotification
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.MessageSource
import java.time.Instant
import java.util.*

class PushNotificationMapperTest : FreeSpec({
    "asGraphQL" - {
        val createdTime = Instant.now()

        "HeartRateMatch notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlHeartRateMatchPushNotificationData(10f, "user1")
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.HeartRateMatchData(10f, "user1")
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "HighHeartRate notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlHighHeartRatePushNotificationData(10f, "user1")
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.HighHeartRateData("user1", 10f)
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "LowHeartRate notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlLowHeartRatePushNotificationData(10f, "user1")
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.LowHeartRateData("user1", 10f)
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "Ban notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlBannedPushNotificationData("user1")
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.BanData("user1")
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "HighOwnHeartRate notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlHighOwnHeartRatePushNotificationData(10f)
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.HighOwnHeartRateData(10f)
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "LowOwnHeartRate notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlLowOwnHeartRatePushNotificationData(10f)
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.LowOwnHeartRateData(10f)
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "NewSubscriber notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = GraphqlNewSubscriberPushNotificationData("subscriptionId")
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.NewSubscriberData("subscriptionId", "subscriberUserId")
            )

            pushNotification.asGraphQL() shouldBe expected
        }

        "FailedToRefund notification" {
            val expected = GraphqlPushNotification(
                id = "notificationId",
                created = createdTime,
                data = null
            )

            val pushNotification = PushNotification(
                id = "notificationId",
                userId = "userId",
                created = createdTime,
                data = PushNotification.Data.FailedToRefundData
            )

            pushNotification.asGraphQL() shouldBe expected
        }
    }

    "getMessageText" - {
        val messageArbitrary = arbitrary { PushNotificationMessage.Message(
            code = Arb.string(codepoints = Codepoint.az(), range = 1..30).bind(),
            arguments = Arb.list(Arb.string(), range = 0..4).bind()
        ) }

        "with known locale" {
            val result = "Example result"
            val messageSource = mockk<MessageSource>() {
                every { getMessage(any(), any(), any()) } returns result
            }

            val localeArbitrary = Arb.element(
                PushNotificationLocale.RU to Locale("RU"),
                PushNotificationLocale.EN to Locale.ROOT
            )

            checkAll(10, messageArbitrary, localeArbitrary) {
                    message, (localeToPass, localeToExpect) ->
                messageSource.getMessageText(message, localeToPass) shouldBe result

                verify(exactly = 1) {
                    messageSource.getMessage(message.code, message.arguments.toTypedArray(), localeToExpect)
                }
            }
        }

        "with unknown locale" {
            shouldThrow<IllegalArgumentException> {
                val message = messageArbitrary.take(1).iterator().next()
                mockk<MessageSource>().getMessageText(message, Locale("abc"))
            }
        }
    }
})
