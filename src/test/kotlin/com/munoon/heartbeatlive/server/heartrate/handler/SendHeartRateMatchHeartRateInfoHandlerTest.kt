package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.push.HeartRateMatchNotificationData
import com.munoon.heartbeatlive.server.push.PushNotification
import com.munoon.heartbeatlive.server.push.repository.PushNotificationRepository
import com.munoon.heartbeatlive.server.push.service.PushNotificationService
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import com.ninjasquad.springmockk.MockkBean
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
internal class SendHeartRateMatchHeartRateInfoHandlerTest : AbstractTest() {
    @Autowired
    private lateinit var handler: SendHeartRateMatchHeartRateInfoHandler

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var subscriptionRepository: SubscriptionRepository

    @Autowired
    private lateinit var notificationRepository: PushNotificationRepository

    @MockkBean
    private lateinit var pushNotificationService: PushNotificationService

    @Test
    fun handleHeartRateInfo(): Unit = runBlocking {
        coEvery { pushNotificationService.sendNotifications(any()) } returns Unit

        /**
         * user1 + user2 (both should receive)
         * user1 + user3 (user3 notifications off, user1 should receive)
         * user1 + user4 (user1 notifications off, user4 should receive)
         * user1 + user5 (both notifications off)
         * user1 + user6 (fresh heart rate not match)
         * user1 + user12 (fresh heart rate not match, but last heart rate is null)
         * user1 + user7 (only user1 subscribed to user7)
         * user1 + user8 (only user8 subscribed to user1)
         * user1 + user9 (notification was recently sent to both)
         * user1 + user10 (notification was recently sent to user1, user10 should receive)
         * user1 + user11 (notification was recently sent to user11, user1 should receive)
         */

        createUser(id = "user1", "User 1")
        createUser(id = "user2", "User 2", User.HeartRate(null, Instant.now()),
            User.HeartRate(60, Instant.now().minusSeconds(5)),
            User.HeartRate(50, Instant.now().minusSeconds(10)),
            User.HeartRate(70, Instant.now().minusSeconds(15)))
        createUser(id = "user3", "User 3", User.HeartRate(50, Instant.now()))
        createUser(id = "user4", "User 4", User.HeartRate(50, Instant.now()))
        createUser(id = "user5", "User 5", User.HeartRate(50, Instant.now()))
        createUser(id = "user6", "User 6", User.HeartRate(60, Instant.now()),
            User.HeartRate(50, Instant.now().minusSeconds(99999)))
        createUser(id = "user12", "User 12", User.HeartRate(null, Instant.now()),
            User.HeartRate(50, Instant.now().minusSeconds(99999)))
        createUser(id = "user7", "User 7", User.HeartRate(50, Instant.now()))
        createUser(id = "user8", "User 8", User.HeartRate(50, Instant.now()))
        createUser(id = "user9", "User 9", User.HeartRate(50, Instant.now()))
        createUser(id = "user10", "User 10", User.HeartRate(50, Instant.now()))
        createUser(id = "user11", "User 11", User.HeartRate(50, Instant.now()))

        val subscription2to1 =
            createSubscription(userId = "user1", subscriberUserId = "user2", receiveNotification = true)
        val subscription1to2 =
            createSubscription(userId = "user2", subscriberUserId = "user1", receiveNotification = true)

        createSubscription(userId = "user1", subscriberUserId = "user3", receiveNotification = false)
        val subscription1to3 =
            createSubscription(userId = "user3", subscriberUserId = "user1", receiveNotification = true)

        val subscription4to1 =
            createSubscription(userId = "user1", subscriberUserId = "user4", receiveNotification = true)
        createSubscription(userId = "user4", subscriberUserId = "user1", receiveNotification = false)

        createSubscription(userId = "user1", subscriberUserId = "user5", receiveNotification = false)
        createSubscription(userId = "user5", subscriberUserId = "user1", receiveNotification = false)

        createSubscription(userId = "user1", subscriberUserId = "user6", receiveNotification = true)
        createSubscription(userId = "user6", subscriberUserId = "user1", receiveNotification = true)

        createSubscription(userId = "user1", subscriberUserId = "user12", receiveNotification = true)
        createSubscription(userId = "user12", subscriberUserId = "user1", receiveNotification = true)

        createSubscription(userId = "user7", subscriberUserId = "user1", receiveNotification = true)
        createSubscription(userId = "user1", subscriberUserId = "user8", receiveNotification = true)

        createSubscription(userId = "user1", subscriberUserId = "user9", receiveNotification = true)
        createSubscription(userId = "user9", subscriberUserId = "user1", receiveNotification = true)

        val subscription10to1 =
            createSubscription(userId = "user1", subscriberUserId = "user10", receiveNotification = true)
        createSubscription(userId = "user10", subscriberUserId = "user1", receiveNotification = true)

        createSubscription(userId = "user1", subscriberUserId = "user11", receiveNotification = true)
        val subscription1to11 =
            createSubscription(userId = "user11", subscriberUserId = "user1", receiveNotification = true)

        val expectedNotifications = listOf(
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user1",
                matchWithUserId = "user2",
                matchWithUserDisplayName = "User 2",
                subscriptionId = subscription1to2.id!!
            ),
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user2",
                matchWithUserId = "user1",
                matchWithUserDisplayName = "User 1",
                subscriptionId = subscription2to1.id!!
            ),
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user1",
                matchWithUserId = "user3",
                matchWithUserDisplayName = "User 3",
                subscriptionId = subscription1to3.id!!
            ),
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user4",
                matchWithUserId = "user1",
                matchWithUserDisplayName = "User 1",
                subscriptionId = subscription4to1.id!!
            ),
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user10",
                matchWithUserId = "user1",
                matchWithUserDisplayName = "User 1",
                subscriptionId = subscription10to1.id!!
            ),
            HeartRateMatchNotificationData(
                heartRate = 50f,
                userId = "user1",
                matchWithUserId = "user11",
                matchWithUserDisplayName = "User 11",
                subscriptionId = subscription1to11.id!!
            )
        )

        createHeartRateMatchNotification(userA = "user1", userB = "user2",
            created = Instant.now().minus(10, ChronoUnit.DAYS))
        createHeartRateMatchNotification(userA = "user1", userB = "user9", created = Instant.now())
        createHeartRateMatchNotification(userA = "user3", userB = "userX", created = Instant.now())

        notificationRepository.save(PushNotification(userId = "user1",
            data = PushNotification.Data.HeartRateMatchData(heartRate = 70f, matchWithUserId = "user10")))
        notificationRepository.save(PushNotification(userId = "user11",
            data = PushNotification.Data.HeartRateMatchData(heartRate = 70f, matchWithUserId = "user1")))

        notificationRepository.save(PushNotification(userId = "user1",
            data = PushNotification.Data.HighOwnHeartRateData(heartRate = 120f)))
        notificationRepository.save(PushNotification(userId = "user3",
            data = PushNotification.Data.HighOwnHeartRateData(heartRate = 120f)))

        handler.handleHeartRateInfo(userId = "user1", heartRate = 50f)

        coVerify(exactly = 1) { pushNotificationService.sendNotifications(expectedNotifications) }
    }

    private suspend fun createUser(id: String, displayName: String, vararg heartRates: User.HeartRate): User {
        return userRepository.save(User(id, displayName = displayName, email = null,
            emailVerified = false, heartRates = heartRates.toList()))
    }

    private suspend fun createSubscription(
        userId: String, subscriberUserId: String, receiveNotification: Boolean): Subscription {
        return subscriptionRepository.save(Subscription(
            userId = userId, subscriberUserId = subscriberUserId,
            receiveHeartRateMatchNotifications = receiveNotification))
    }

    private suspend fun createHeartRateMatchNotification(userA: String, userB: String, created: Instant) {
        notificationRepository.save(PushNotification(userId = userA, created = created,
            data = PushNotification.Data.HeartRateMatchData(heartRate = 70f, matchWithUserId = userB)))

        notificationRepository.save(PushNotification(userId = userB, created = created,
            data = PushNotification.Data.HeartRateMatchData(heartRate = 70f, matchWithUserId = userA)))
    }
}