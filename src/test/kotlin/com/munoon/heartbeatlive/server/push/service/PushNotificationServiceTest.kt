package com.munoon.heartbeatlive.server.push.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.onesignal.OneSignalApiException
import com.munoon.heartbeatlive.server.push.HighOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationData
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.PushNotificationMessage
import com.munoon.heartbeatlive.server.push.PushNotificationNotFoundByIdException
import com.munoon.heartbeatlive.server.push.model.PushNotificationPriority
import com.munoon.heartbeatlive.server.push.model.SendPushNotificationData
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.sender.PushNotificationSender
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import org.springframework.data.domain.PageRequest

@SpringBootTest
class PushNotificationServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: PushNotificationService

    @Autowired
    private lateinit var repository: PushNotificationRepository

    @MockkBean
    private lateinit var pushNotificationSender: PushNotificationSender

    @MockkBean
    private lateinit var messageSource: MessageSource

    @Test
    fun sendNotification(): Unit = runBlocking {
        val expectedSendNotificationData = listOf(
            SendPushNotificationData(
                userIds = setOf("user1"),
                title = mapOf(
                    PushNotificationLocale.EN to "Message 1",
                    PushNotificationLocale.RU to "Message 1"
                ),
                content = mapOf(
                    PushNotificationLocale.EN to "Message 2",
                    PushNotificationLocale.RU to "Message 2"
                ),
                metadata = emptyMap(),
                priority = PushNotificationPriority.MEDIUM
            ),
            SendPushNotificationData(
                userIds = setOf("user2"),
                title = mapOf(
                    PushNotificationLocale.EN to "Message 1",
                    PushNotificationLocale.RU to "Message 1"
                ),
                content = mapOf(
                    PushNotificationLocale.EN to "Message 2",
                    PushNotificationLocale.RU to "Message 2"
                ),
                metadata = emptyMap(),
                priority = PushNotificationPriority.MEDIUM
            ),
            SendPushNotificationData(
                userIds = setOf("user3"),
                title = mapOf(
                    PushNotificationLocale.EN to "Message 3",
                    PushNotificationLocale.RU to "Message 3"
                ),
                content = mapOf(
                    PushNotificationLocale.EN to "Message 4",
                    PushNotificationLocale.RU to "Message 4"
                ),
                metadata = emptyMap(),
                priority = PushNotificationPriority.HIGH
            ),
            SendPushNotificationData(
                userIds = setOf("user4"),
                title = mapOf(
                    PushNotificationLocale.EN to "Message 3",
                    PushNotificationLocale.RU to "Message 3"
                ),
                content = mapOf(
                    PushNotificationLocale.EN to "Message 4",
                    PushNotificationLocale.RU to "Message 4"
                ),
                metadata = emptyMap(),
                priority = PushNotificationPriority.HIGH
            ),
            SendPushNotificationData(
                userIds = setOf("user5"),
                title = mapOf(
                    PushNotificationLocale.EN to "Message 3",
                    PushNotificationLocale.RU to "Message 3"
                ),
                content = mapOf(
                    PushNotificationLocale.EN to "Message 4",
                    PushNotificationLocale.RU to "Message 4"
                ),
                metadata = emptyMap(),
                priority = PushNotificationPriority.HIGH
            )
        )

        val expectedPushNotifications = listOf(
            PushNotification(
                userId = "user1",
                data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscription1", "subscriberUserId")
            ),
            PushNotification(
                userId = "user2",
                data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscription1", "subscriberUserId")
            ),
            PushNotification(
                userId = "user3",
                data = PushNotification.Data.BanData(bannedByUserId = "userId")
            ),
            PushNotification(
                userId = "user4",
                data = PushNotification.Data.BanData(bannedByUserId = "userId")
            ),
            PushNotification(
                userId = "user5",
                data = PushNotification.Data.BanData(bannedByUserId = "userId")
            )
        )

        every { messageSource.getMessage("test_push_notification_1", any(), any()) } returns
                "Message 1"
        every { messageSource.getMessage("test_push_notification_2", any(), any()) } returns
                "Message 2"
        every { messageSource.getMessage("test_push_notification_3", any(), any()) } returns
                "Message 3"
        every { messageSource.getMessage("test_push_notification_4", any(), any()) } returns
                "Message 4"
        coEvery { pushNotificationSender.sendNotification(any()) } answers {
            if (firstArg<SendPushNotificationData>().userIds == setOf("user3")) throw OneSignalApiException("Exception")
            else Unit
        }

        val notification1 = buildMockedPushNotificationData(
            mockedTitle = PushNotificationMessage.Message(
                code = "test_push_notification_1",
                "arg1", 2
            ),
            mockedContent = PushNotificationMessage.Message(
                code = "test_push_notification_2",
                "arg3", 4
            ),
            mockedUserIds = setOf("user1", "user2"),
            mockedNotification = PushNotification.Data.NewSubscriberData(
                subscriptionId = "subscription1",
                subscriberUserId = "subscriberUserId"
            ),
            mockedPriority = PushNotificationPriority.MEDIUM
        )

        val notification2 = buildMockedPushNotificationData(
            mockedTitle = PushNotificationMessage.Message(
                code = "test_push_notification_3",
                "arg5", 6
            ),
            mockedContent = PushNotificationMessage.Message(
                code = "test_push_notification_4",
                "arg7", 8
            ),
            mockedUserIds = setOf("user3", "user4", "user5"),
            mockedNotification = PushNotification.Data.BanData(
                bannedByUserId = "userId"
            ),
            mockedPriority = PushNotificationPriority.HIGH
        )

        service.sendNotifications(notification1, notification2)

        val actualNotifications = repository.findAll().toList(arrayListOf())
        actualNotifications.forEach {
            val expected = expectedPushNotifications.find { item -> item.userId == it.userId }
            it.shouldBeEqualToIgnoringFields(expected!!, PushNotification::created, PushNotification::id)
        }

        val notificationIds = actualNotifications.mapTo(mutableSetOf()) { JsonPrimitive(it.id) }

        coVerify(exactly = 5) { pushNotificationSender.sendNotification(match {
            val expected = expectedSendNotificationData.find { item -> item.userIds == it.userIds }
            it.shouldBeEqualToIgnoringFields(expected!!, SendPushNotificationData::metadata)
            it.metadata shouldHaveSize 1
            it.metadata shouldContainKey "heart_beat_live:notification_id"
            it.metadata["heart_beat_live:notification_id"] shouldBeIn notificationIds

            true
        }) }
    }

    @Test
    fun getPushNotificationsByUserId() {
        every { messageSource.getMessage(any(), any(), any()) } returns "Message"
        coEvery { pushNotificationSender.sendNotification(any()) } returns Unit

        val expected = listOf(
            PushNotification(
                userId = "user3",
                data = PushNotification.Data.LowOwnHeartRateData(10f)
            ),
            PushNotification(
                userId = "user3",
                data = PushNotification.Data.HighOwnHeartRateData(10f)
            )
        )

        runBlocking { service.sendNotifications(
            LowHeartRateNotificationData(
                heartRate = 10f,
                heartRateOwnerUserId = "user2",
                heartRateOwnerUserDisplayName = "Display Name",
                userIds = setOf("user1", "user2")
            ),
            LowOwnHeartRateNotificationData(
                heartRate = 10f,
                userId = "user3"
            ),
            HighOwnHeartRateNotificationData(
                heartRate = 10f,
                userId = "user3"
            )
        ) }

        val pageRequest = PageRequest.of(0, 10)
        val actual = runBlocking { service.getPushNotificationsByUserId("user3", pageRequest) }

        actual.totalItemsCount shouldBe 2
        runBlocking {
            assertThat(actual.data.toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("id", "created")
                .isEqualTo(expected)
        }
    }

    @Test
    fun getPushNotificationById() {
        val expected = runBlocking { repository.save(PushNotification(
            userId = "userId",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscription1", "subscriberUserId")
        )) }

        val actual = runBlocking { service.getPushNotificationById(expected.id) }
        actual.shouldBeEqualToIgnoringFields(expected, PushNotification::created)
    }

    @Test
    fun `getPushNotificationById - not found`() {
        shouldThrow<PushNotificationNotFoundByIdException> {
            runBlocking { service.getPushNotificationById("unknownId") }
        } shouldBe PushNotificationNotFoundByIdException("unknownId")
    }

    private fun buildMockedPushNotificationData(
        mockedTitle: PushNotificationMessage.Message,
        mockedContent: PushNotificationMessage.Message,
        mockedUserIds: Set<String>,
        mockedNotification: PushNotification.Data,
        mockedPriority: PushNotificationPriority
    ) = mockk<PushNotificationData>() {
        every { message } returns mockk() {
            every { title } returns mockedTitle
            every { content } returns mockedContent
        }
        every { userIds } returns mockedUserIds
        every { notification } returns mockedNotification
        every { priority } returns mockedPriority
    }
}