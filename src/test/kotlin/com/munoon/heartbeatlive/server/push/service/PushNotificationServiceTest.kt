package com.munoon.heartbeatlive.server.push.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.push.NewSubscriptionPushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.sender.push.PushNotificationSender
import com.ninjasquad.springmockk.MockkBean
import io.kotest.common.runBlocking
import io.kotest.equals.ReflectionIgnoringFieldsEquality
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.maps.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.uuid
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest
class PushNotificationServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: PushNotificationService

    @Autowired
    private lateinit var repository: PushNotificationRepository

    @MockkBean
    private lateinit var pushNotificationSender: PushNotificationSender

    @Test
    fun sendNotification() {
        val notificationsArbitrary = arbitrary { NewSubscriptionPushNotificationData(
            userId = Arb.uuid().map { it.toString() }.bind(),
            subscriptionId = Arb.uuid().map { it.toString() }.bind(),
            subscriberDisplayName = Arb.string(minSize = 2, maxSize = 15).bind()
        ) }

        coEvery { pushNotificationSender.sendNotification(any()) } returns Unit
        runBlocking { checkAll(1, notificationsArbitrary) { notification ->
            val subscriberDisplayName = notification.subscriberDisplayName

            service.sendNotification(notification)

            repository.findAll().toList(arrayListOf()).shouldContain(PushNotification(
                userId = notification.userId,
                data = PushNotification.Data.NewSubscriberData(notification.subscriptionId)
            ), comparator = ReflectionIgnoringFieldsEquality(PushNotification::id, arrayOf(PushNotification::created)))

            coVerify { pushNotificationSender.sendNotification(match {
                it.shouldBeEqualToIgnoringFields(SendPushNotificationData(
                    userId = notification.userId,
                    metadata = mapOf("subscription_id" to JsonPrimitive(notification.subscriptionId)),
                    title = emptyMap(), content = emptyMap()
                ), SendPushNotificationData::content, SendPushNotificationData::title)
                it.title shouldContain (Locale("en") to "New subscriber")
                it.title shouldContain (Locale("ru") to "Новый подписчик")
                it.content shouldContain (Locale("en") to "$subscriberDisplayName is now subscribing you!")
                it.content shouldContain (Locale("ru") to "$subscriberDisplayName теперь подписан(а) на Вас!")
                true
            }) }
        } }
    }
}