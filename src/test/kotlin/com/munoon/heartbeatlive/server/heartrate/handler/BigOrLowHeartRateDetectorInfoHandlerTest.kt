package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.heartrate.BigOrLowHeartRateDetectorInfoHandler
import com.munoon.heartbeatlive.server.heartrate.repository.HighLowHeartRateNotificationRepository
import com.munoon.heartbeatlive.server.push.HighHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.HighOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.LowOwnHeartRateNotificationData
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.service.UserSubscribersLoaderService
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.common.runBlocking
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class BigOrLowHeartRateDetectorInfoHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var handler: BigOrLowHeartRateDetectorInfoHandler

    @MockkBean
    private lateinit var pushNotificationService: PushNotificationService

    @MockkBean
    private lateinit var userSubscribersLoaderService: UserSubscribersLoaderService

    @MockkBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var repository: HighLowHeartRateNotificationRepository

    @Test
    fun `handleHeartRateInfo - send low pulse`(): Unit = runBlocking {
        val userArbitrary = arbitrary { User(
            id = Arb.string().bind(),
            displayName = Arb.string().bind(),
            email = null, emailVerified = false
        ) }
        coEvery { pushNotificationService.sendNotifications(*anyVararg()) } returns Unit

        checkAll(1,
            userArbitrary,
            Arb.float(min = 0f, max = 30f),
            Arb.map(Arb.string(), Arb.string(), minSize = 1, maxSize = 5)
        ) { user, heartRate, subscribers ->
            val expectedNotificationOne = LowHeartRateNotificationData(
                heartRate = heartRate,
                heartRateOwnerUserId = user.id,
                heartRateOwnerUserDisplayName = user.displayName!!,
                userIds = subscribers.values.toSet()
            )
            val expectedNotificationTwo = LowOwnHeartRateNotificationData(
                heartRate = heartRate,
                userId = user.id
            )

            every { userSubscribersLoaderService.load(user.id) } returns subscribers
            coEvery { userService.getUserById(user.id) } returns user

            handler.handleHeartRateInfo(user.id, heartRate)

            coVerify { pushNotificationService.sendNotifications(expectedNotificationOne, expectedNotificationTwo) }
            repository.existsByUserId(user.id) shouldBe true
        }
    }

    @Test
    fun `handleHeartRateInfo - send high pulse`(): Unit = runBlocking {
        val userArbitrary = arbitrary { User(
            id = Arb.string().bind(),
            displayName = Arb.string().bind(),
            email = null, emailVerified = false
        ) }
        coEvery { pushNotificationService.sendNotifications(*anyVararg()) } returns Unit

        checkAll(1,
            userArbitrary,
            Arb.float(min = 180f, max = 999f),
            Arb.map(Arb.string(), Arb.string(), minSize = 1, maxSize = 5)
        ) { user, heartRate, subscribers ->
            val expectedNotificationOne = HighHeartRateNotificationData(
                heartRate = heartRate,
                heartRateOwnerUserId = user.id,
                heartRateOwnerUserDisplayName = user.displayName!!,
                userIds = subscribers.values.toSet()
            )
            val expectedNotificationTwo = HighOwnHeartRateNotificationData(
                heartRate = heartRate,
                userId = user.id
            )

            every { userSubscribersLoaderService.load(user.id) } returns subscribers
            coEvery { userService.getUserById(user.id) } returns user

            handler.handleHeartRateInfo(user.id, heartRate)

            coVerify { pushNotificationService.sendNotifications(expectedNotificationOne, expectedNotificationTwo) }
            repository.existsByUserId(user.id) shouldBe true
        }
    }

    @Test
    fun `handleHeartRateInfo - already sent`(): Unit = runBlocking {
        val user = arbitrary { User(
            id = Arb.string(minSize = 1, maxSize = 20, codepoints = Codepoint.Companion.alphanumeric()).bind(),
            displayName = Arb.string().bind(),
            email = null, emailVerified = false
        ) }.take(1).iterator().next()

        coEvery { userService.getUserById(user.id) } returns user
        coEvery { pushNotificationService.sendNotifications(*anyVararg()) } returns Unit
        every { userSubscribersLoaderService.load(user.id) } returns
                Arb.map(Arb.string(), Arb.string(), minSize = 1, maxSize = 5).take(1).iterator().next()

        checkAll(3, Arb.float(min = 180f, max = 999f)) { heartRate ->
            handler.handleHeartRateInfo(user.id, heartRate)
        }

        coVerify(exactly = 1) { pushNotificationService.sendNotifications(*anyVararg()) }
    }

    @Test
    fun `filter - high pulse`(): Unit = runBlocking {
        checkAll(100, Arb.float(min = 200f, max = 1000f), Arb.string()) { heartRate, userId ->
            handler.filter(userId, heartRate) shouldBe true
        }
    }

    @Test
    fun `filter - low pulse`(): Unit = runBlocking {
        checkAll(100, Arb.float(min = 0f, max = 30f), Arb.string()) { heartRate, userId ->
            handler.filter(userId, heartRate) shouldBe true
        }
    }

    @Test
    fun `filter - normal pulse`(): Unit = runBlocking {
        checkAll(100, Arb.float(min = 31f, max = 179f), Arb.string()) { heartRate, userId ->
            handler.filter(userId, heartRate) shouldBe false
        }
    }
}