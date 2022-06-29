package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.ban.UserBanedByOtherUserException
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingExpiredException
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.SelfSubscriptionAttemptException
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionEvent
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.UserSubscribersLimitExceededException
import com.munoon.heartbeatlive.server.subscription.UserSubscriptionsLimitExceededException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.model.GraphqlSubscribeOptionsInput
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
@RecordApplicationEvents
internal class SubscriptionServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: SubscriptionService

    @Autowired
    private lateinit var repository: SubscriptionRepository

    @MockkBean
    private lateinit var heartBeatSharingService: HeartBeatSharingService

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @MockkBean
    private lateinit var userBanService: UserBanService

    @SpykBean
    private lateinit var userService: UserService

    @Autowired
    private lateinit var events: ApplicationEvents

    @BeforeEach
    fun setUpMocks() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = null,
            emailVerified = false
        )

        coEvery { userBanService.checkUserBanned(any(), any()) } returns false
    }

    @Test
    fun subscribeBySharingCode() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )
        runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(
            id = "user2", email = null, emailVerified = false)) }

        val options = GraphqlSubscribeOptionsInput(receiveHeartRateMatchNotifications = true)
        val subscription = runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2",
            userSubscriptionPlan = UserSubscriptionPlan.FREE, options) }

        val expectedSubscription = Subscription(
            id = subscription.id!!,
            userId = "user1",
            subscriberUserId = "user2",
            created = subscription.created,
            receiveHeartRateMatchNotifications = true
        )

        assertThat(subscription).usingRecursiveComparison().isEqualTo(expectedSubscription)
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expectedSubscription))
        }

        coVerify(exactly = 1) { heartBeatSharingService.getSharingCodeByPublicCode(any()) }
        coVerify(exactly = 1) { userBanService.checkUserBanned("user2", "user1") }

        events.stream(SubscriptionEvent::class.java).toList() shouldContainExactly
                listOf(SubscriptionEvent.SubscriptionCreatedEvent(expectedSubscription))
    }

    @Test
    fun `subscribeBySharingCode - self subscription`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        val options = GraphqlSubscribeOptionsInput()
        assertThatThrownBy { runBlocking {
            service.subscribeBySharingCode(code = "ABC123", userId = "user1", UserSubscriptionPlan.FREE, options)
        } }.isExactlyInstanceOf(SelfSubscriptionAttemptException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - sharing code expired`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = OffsetDateTime.now().minus(Duration.ofDays(10)).toInstant()
        )

        val options = GraphqlSubscribeOptionsInput()
        assertThatThrownBy { runBlocking {
            service.subscribeBySharingCode(code = "ABC123", userId = "user2", UserSubscriptionPlan.FREE, options)
        } }.isExactlyInstanceOf(HeartBeatSharingExpiredException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - user have too many subscribers`() {
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user4",
            receiveHeartRateMatchNotifications = false)) }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        val options = GraphqlSubscribeOptionsInput()
        assertThatThrownBy { runBlocking {
            service.subscribeBySharingCode(code = "ABC123", userId = "user5", UserSubscriptionPlan.FREE, options)
        } }.isExactlyInstanceOf(UserSubscribersLimitExceededException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - user subscribed on too many users`() {
        for (i in 2..10) {
            runBlocking { repository.save(Subscription(userId = "user$i", subscriberUserId = "user1",
                receiveHeartRateMatchNotifications = false)) }
        }
        runBlocking { assertThat(repository.count()).isEqualTo(9) }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user11",
            expiredAt = null
        )

        val options = GraphqlSubscribeOptionsInput()
        assertThatThrownBy { runBlocking {
            service.subscribeBySharingCode(code = "ABC123", userId = "user1", UserSubscriptionPlan.FREE, options)
        } }.isExactlyInstanceOf(UserSubscriptionsLimitExceededException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - user is banned by sharing code owner`() {
        coEvery { userBanService.checkUserBanned("user2", "user1") } returns true
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        val options = GraphqlSubscribeOptionsInput()
        assertThatThrownBy { runBlocking {
            service.subscribeBySharingCode(code = "ABC123", userId = "user2", UserSubscriptionPlan.FREE, options)
        } }.isEqualTo(UserBanedByOtherUserException(userId = "user2", bannedByUserId = "user1"))

        coVerify(exactly = 1) { userBanService.checkUserBanned("user2", "user1") }
    }

    @Test
    fun `subscribeBySharingCode - already exist`() {
        coEvery { userService.getUserById(any()) } returns User(
            id = "userId",
            displayName = null,
            email = null,
            emailVerified = false,
            subscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = Instant.now().plusSeconds(60),
                startAt = Instant.now(),
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "stripeSubscription1",
                    paymentIntentId = "stripePaymentIntent1"
                )
            )
        )

        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { assertThat(repository.count()).isOne }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        val options = GraphqlSubscribeOptionsInput()
        val newSubscription = runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2",
            userSubscriptionPlan = UserSubscriptionPlan.PRO, options) }
        assertThat(newSubscription).usingRecursiveComparison().ignoringFields("created").isEqualTo(subscription)

        runBlocking { assertThat(repository.count()).isOne }
    }

    @Test
    fun `unsubscribeFromUserById - without verification`() {
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { assertThat(repository.count()).isOne }
        runBlocking { service.unsubscribeFromUserById(subscription.id!!, validateUserId = null) }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `unsubscribeFromUserById - without verification not found`() {
        assertThatThrownBy { runBlocking { service.unsubscribeFromUserById("abc", validateUserId = null) } }
            .isEqualTo(SubscriptionNotFoundByIdException(id = "abc"))
    }

    @Test
    fun `unsubscribeFromUserById - with verification`() {
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { assertThat(repository.count()).isOne }
        runBlocking { service.unsubscribeFromUserById(subscription.id!!, validateUserId = "user2") }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `unsubscribeFromUserById - with verification not found`() {
        assertThatThrownBy { runBlocking { service.unsubscribeFromUserById("abc", validateUserId = "user1") } }
            .isEqualTo(SubscriptionNotFoundByIdException(id = "abc"))
    }

    @Test
    fun `unsubscribeFromUserById - with verification not verified`() {
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { assertThat(repository.count()).isOne }

        assertThatThrownBy { runBlocking {
            service.unsubscribeFromUserById(subscription.id!!, validateUserId = "user1")
        } }.isEqualTo(SubscriptionNotFoundByIdException(id = subscription.id!!))

        runBlocking { assertThat(repository.count()).isOne }
    }

    @Test
    fun getSubscriptionById() {
        val expected = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }

        val actual = runBlocking { service.getSubscriptionById(expected.id!!) }

        assertThat(actual).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
    }

    @Test
    fun `getSubscriptionById - not found`() {
        assertThatThrownBy { runBlocking { service.getSubscriptionById("abc") } }
            .isEqualTo(SubscriptionNotFoundByIdException(id = "abc"))
    }

    @Test
    fun getSubscribers() {
        val expected1 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) } // not expected
        runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) } // not expected

        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        val actual = runBlocking { service.getSubscribers("user1", pageRequest) }

        assertThat(actual.totalItemsCount).isEqualTo(2)
        runBlocking {
            assertThat(actual.data.toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected1, expected2))
        }
    }

    @Test
    fun getSubscriptions() {
        val expected1 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) } // not expected
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) } // not expected

        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        val actual = runBlocking { service.getSubscriptions("user1", pageRequest) }

        assertThat(actual.totalItemsCount).isEqualTo(2)
        runBlocking {
            assertThat(actual.data.toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected1, expected2))
        }
    }

    @Test
    fun `checkUserHaveMaximumSubscribers - true`() {
        for (i in 1..10) {
            runBlocking { repository.save(Subscription(userId = "userId", subscriberUserId = "user$i",
                receiveHeartRateMatchNotifications = false)) }
        }
        runBlocking { assertThat(repository.count()).isEqualTo(10) }

        val result = runBlocking { service.checkUserHaveMaximumSubscribers("userId") }
        assertThat(result).isTrue
    }

    @Test
    fun `checkUserHaveMaximumSubscribers - false`() {
        val result = runBlocking { service.checkUserHaveMaximumSubscribers("userId") }
        assertThat(result).isFalse
    }

    @Test
    fun `checkUserHaveMaximumSubscriptions - true`() {
        for (i in 1..10) {
            runBlocking { repository.save(Subscription(userId = "user$i", subscriberUserId = "userId",
                receiveHeartRateMatchNotifications = false)) }
        }
        runBlocking { assertThat(repository.count()).isEqualTo(10) }

        val result = runBlocking {
            service.checkUserHaveMaximumSubscriptions("userId", UserSubscriptionPlan.FREE)
        }
        assertThat(result).isTrue
    }

    @Test
    fun `checkUserHaveMaximumSubscriptions - false`() {
        val result = runBlocking {
            service.checkUserHaveMaximumSubscriptions("userId", UserSubscriptionPlan.FREE)
        }
        assertThat(result).isFalse
    }

    @Test
    fun getAllByIds() {
        val subscription1 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        val subscription2 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }

        val actual = runBlocking {
            service.getAllByIds(setOf(subscription1.id!!, subscription2.id!!)).toList(arrayListOf())
        }

        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("created")
            .isEqualTo(listOf(subscription1, subscription2))
    }

    @Test
    fun handleUserDeletedEvent() {
        every { userBanService.handleUserDeletedEvent(any()) } returns Unit
        every { heartBeatSharingService.handleUserDeletedEvent(any()) } returns Unit
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) }
        val notDelete = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) }

        runBlocking { assertThat(repository.count()).isEqualTo(3) }

        eventPublisher.publishEvent(UserEvents.UserDeletedEvent("user1", updateFirebaseState = false))

        runBlocking { assertThat(repository.count()).isEqualTo(1) }
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(notDelete))
        }
    }

    @Test
    fun handleUserBannedEvent() {
        every { userBanService.handleUserDeletedEvent(any()) } returns Unit
        runBlocking { userService.createUser(
            GraphqlFirebaseCreateUserInput(id = "user1", email = null, emailVerified = false)) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        val expected1 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1",
            receiveHeartRateMatchNotifications = false)) }
        val expected3 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) }
        val expected4 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user3",
            receiveHeartRateMatchNotifications = false)) }
        val expected5 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user2",
            receiveHeartRateMatchNotifications = false)) }
        runBlocking { assertThat(repository.count()).isEqualTo(6) }

        eventPublisher.publishEvent(UserBanEvents.UserBannedEvent("user2", "user1"))

        runBlocking {
            assertThat(repository.count()).isEqualTo(5)

            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected1, expected2, expected3, expected4, expected5))
        }
    }
}