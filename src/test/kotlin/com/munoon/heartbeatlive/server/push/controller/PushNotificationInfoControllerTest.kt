package com.munoon.heartbeatlive.server.push.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.push.BanPushNotificationMessage
import com.munoon.heartbeatlive.server.push.FailedToRefundPushNotificationMessage
import com.munoon.heartbeatlive.server.push.HeartRateMatchPushNotificationMessage
import com.munoon.heartbeatlive.server.push.HighHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.LowHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.LowOwnHeartRatePushNotificationMessage
import com.munoon.heartbeatlive.server.push.NewSubscriptionPushNotificationMessage
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.PushNotificationLocale
import com.munoon.heartbeatlive.server.push.PushNotificationMapper.getMessageText
import com.munoon.heartbeatlive.server.push.model.GraphqlPushNotificationInfo
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.user.UserTestUtils
import com.munoon.heartbeatlive.server.user.service.UserService
import com.munoon.heartbeatlive.server.utils.AuthTestUtils.withUser
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.language
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.common.runBlocking
import io.kotest.property.checkAll
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.MessageSource
import java.util.*

@SpringBootTest
internal class PushNotificationInfoControllerTest : AbstractGraphqlHttpTest() {
    private companion object {
        val expectedInfo = GraphqlPushNotificationInfo(
            title = "Message",
            content = "Message"
        )
    }

    @MockkBean
    private lateinit var service: PushNotificationService

    @SpykBean
    private lateinit var messageSource: MessageSource

    @MockkBean
    private lateinit var userService: UserService

    @BeforeEach
    fun setUpMessageSource() {
        every { messageSource.getMessage(any(), any(), any()) } returns "Message"
    }

    @Test
    fun `getPushNotificationInfo - high own heart rate (locale RU)`() {
        val expected = GraphqlPushNotificationInfo(
            title = "❗️ У вас слишком высокий пульс",
            content = "Ваш пульс 300 ударов в минуту."
        )

        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.HighOwnHeartRateData(heartRate = 300f)
        )

        clearMocks(messageSource)
        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1").language("ru")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expected)

        verify(exactly = 2) { messageSource.getMessage(any(), any(), PushNotificationLocale.RU) }
    }

    @Test
    fun `getPushNotificationInfo - high own heart rate (default locale)`() {
        val expected = GraphqlPushNotificationInfo(
            title = "❗️ You have a high pulse",
            content = "Your pulse is 300 bpm."
        )

        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.HighOwnHeartRateData(heartRate = 300f)
        )

        clearMocks(messageSource)
        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expected)

        verify(exactly = 2) { messageSource.getMessage(any(), any(), Locale.ROOT) }
    }

    @Test
    fun `getPushNotificationInfo - low own heart rate`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.LowOwnHeartRateData(heartRate = 5f)
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = LowOwnHeartRatePushNotificationMessage(heartRate = 5f)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
    }

    @Test
    fun `getPushNotificationInfo - ban data`(): Unit = runBlocking {
        checkAll(1, UserTestUtils.userArbitrary) { bannedByUser ->
            val pushNotification = PushNotification(
                userId = "user1",
                data = PushNotification.Data.BanData(bannedByUserId = bannedByUser.id)
            )

            coEvery { service.getPushNotificationById(any()) } returns pushNotification
            every { userService.getUsersByIds(any()) } returns flowOf(bannedByUser)

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

            val message = BanPushNotificationMessage(bannedByUserDisplayName = bannedByUser.displayName)
            verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
            verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
            coVerify(exactly = 1) { userService.getUsersByIds(setOf(bannedByUser.id)) }
        }
    }

    @Test
    fun `getPushNotificationInfo - ban (no user)`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.BanData(bannedByUserId = "userId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification
        coEvery { userService.getUsersByIds(any()) } returns flowOf()

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = BanPushNotificationMessage(bannedByUserDisplayName = null)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun `getPushNotificationInfo - high heart rate`(): Unit = runBlocking {
        checkAll(1, UserTestUtils.userArbitrary) { heartRateOwner ->
            val pushNotification = PushNotification(
                userId = "user1",
                data = PushNotification.Data.HighHeartRateData(heartRate = 300f,
                    heartRateOwnerUserId = heartRateOwner.id)
            )

            coEvery { service.getPushNotificationById(any()) } returns pushNotification
            every { userService.getUsersByIds(any()) } returns flowOf(heartRateOwner)

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

            val message = HighHeartRatePushNotificationMessage(heartRate = 300f,
                heartRateOwnerUserDisplayName = heartRateOwner.displayName)
            verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
            verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
            coVerify(exactly = 1) { userService.getUsersByIds(setOf(heartRateOwner.id)) }
        }
    }

    @Test
    fun `getPushNotificationInfo - high heart rate (no user)`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.HighHeartRateData(heartRate = 300f,
                heartRateOwnerUserId = "userId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification
        every { userService.getUsersByIds(any()) } returns flowOf()

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = HighHeartRatePushNotificationMessage(heartRate = 300f, heartRateOwnerUserDisplayName = null)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun `getPushNotificationInfo - low heart rate`(): Unit = runBlocking {
        checkAll(1, UserTestUtils.userArbitrary) { heartRateOwner ->
            val pushNotification = PushNotification(
                userId = "user1",
                data = PushNotification.Data.LowHeartRateData(heartRate = 300f,
                    heartRateOwnerUserId = heartRateOwner.id)
            )

            coEvery { service.getPushNotificationById(any()) } returns pushNotification
            every { userService.getUsersByIds(any()) } returns flowOf(heartRateOwner)

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

            val message = LowHeartRatePushNotificationMessage(heartRate = 300f,
                heartRateOwnerUserDisplayName = heartRateOwner.displayName)
            verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
            verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
            coVerify(exactly = 1) { userService.getUsersByIds(setOf(heartRateOwner.id)) }
        }
    }

    @Test
    fun `getPushNotificationInfo - low heart rate (no user)`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.LowHeartRateData(heartRate = 300f,
                heartRateOwnerUserId = "userId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification
        every { userService.getUsersByIds(any()) } returns flowOf()

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = LowHeartRatePushNotificationMessage(heartRate = 300f, heartRateOwnerUserDisplayName = null)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun `getPushNotificationInfo - heart rate match`(): Unit = runBlocking {
        checkAll(1, UserTestUtils.userArbitrary) { matchWithUser ->
            val pushNotification = PushNotification(
                userId = "user1",
                data = PushNotification.Data.HeartRateMatchData(heartRate = 300f,
                    matchWithUserId = matchWithUser.id)
            )

            coEvery { service.getPushNotificationById(any()) } returns pushNotification
            every { userService.getUsersByIds(any()) } returns flowOf(matchWithUser)

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

            val message = HeartRateMatchPushNotificationMessage(heartRate = 300f,
                matchWithUserDisplayName = matchWithUser.displayName)
            verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
            verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
            coVerify(exactly = 1) { userService.getUsersByIds(setOf(matchWithUser.id)) }
        }
    }

    @Test
    fun `getPushNotificationInfo - heart rate match (no user)`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.HeartRateMatchData(heartRate = 300f, matchWithUserId = "userId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification
        every { userService.getUsersByIds(any()) } returns flowOf()

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = HeartRateMatchPushNotificationMessage(heartRate = 300f, matchWithUserDisplayName = null)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun `getPushNotificationInfo - new subscriber`(): Unit = runBlocking {
        checkAll(1, UserTestUtils.userArbitrary) { subsccriberUser ->
            val pushNotification = PushNotification(
                userId = "user1",
                data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscription1",
                    subscriberUserId = subsccriberUser.id)
            )

            coEvery { service.getPushNotificationById(any()) } returns pushNotification
            every { userService.getUsersByIds(any()) } returns flowOf(subsccriberUser)

            graphqlTester.withUser(id = "user1")
                .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
                .execute()
                .satisfyNoErrors()
                .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

            val message = NewSubscriptionPushNotificationMessage(subscriberDisplayName = subsccriberUser.displayName)
            verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
            verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
            coVerify(exactly = 1) { userService.getUsersByIds(setOf(subsccriberUser.id)) }
        }
    }

    @Test
    fun `getPushNotificationInfo - new subscriber (no user)`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.NewSubscriberData(subscriptionId = "subscription1",
                subscriberUserId = "userId")
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification
        every { userService.getUsersByIds(any()) } returns flowOf()

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        val message = NewSubscriptionPushNotificationMessage(subscriberDisplayName = null)
        verify(exactly = 1) { messageSource.getMessageText(message.title, PushNotificationLocale.EN) }
        verify(exactly = 1) { messageSource.getMessageText(message.content, PushNotificationLocale.EN) }
        coVerify(exactly = 1) { userService.getUsersByIds(setOf("userId")) }
    }

    @Test
    fun `getPushNotificationInfo - failed to refund`() {
        val pushNotification = PushNotification(
            userId = "user1",
            data = PushNotification.Data.FailedToRefundData
        )

        coEvery { service.getPushNotificationById(any()) } returns pushNotification

        graphqlTester.withUser(id = "user1")
            .document("""
                query {
                    getPushNotificationById(id: "pushNotificationId") {
                        info {
                            title,
                            content
                        }
                    }
                }
            """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getPushNotificationById.info").isEqualsTo(expectedInfo)

        verify(exactly = 1) { messageSource.getMessageText(
            FailedToRefundPushNotificationMessage.title, PushNotificationLocale.EN) }

        verify(exactly = 1) { messageSource.getMessageText(
            FailedToRefundPushNotificationMessage.content, PushNotificationLocale.EN) }
    }
}