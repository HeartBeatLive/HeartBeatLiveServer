package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.ban.UserBanedByOtherUserException
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingExpiredException
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.*
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscription
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.service.AccountSubscriptionService
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.user.UserEvents
import com.ninjasquad.springmockk.MockkBean
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
import java.time.Duration
import java.time.OffsetDateTime

@SpringBootTest
internal class SubscriptionServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: SubscriptionService

    @Autowired
    private lateinit var repository: SubscriptionRepository

    @MockkBean
    private lateinit var heartBeatSharingService: HeartBeatSharingService

    @MockkBean
    private lateinit var accountSubscriptionService: AccountSubscriptionService

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @MockkBean
    private lateinit var userBanService: UserBanService

    @BeforeEach
    fun setUpMocks() {
        coEvery { accountSubscriptionService.getAccountSubscriptionByUserId(any()) } returns AccountSubscription(
            userId = "userId",
            subscriptionPlan = UserSubscriptionPlan.FREE
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

        val subscription = runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2") }

        val expectedSubscription = Subscription(
            id = subscription.id!!,
            userId = "user1",
            subscriberUserId = "user2",
            created = subscription.created
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
    }

    @Test
    fun `subscribeBySharingCode - self subscription`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        assertThatThrownBy { runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user1") } }
            .isExactlyInstanceOf(SelfSubscriptionAttemptException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - sharing code expired`() {
        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = OffsetDateTime.now().minus(Duration.ofDays(10)).toInstant()
        )

        assertThatThrownBy { runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2") } }
            .isExactlyInstanceOf(HeartBeatSharingExpiredException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - user have too many subscribers`() {
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3")) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user4")) }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        assertThatThrownBy { runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user5") } }
            .isExactlyInstanceOf(UserSubscribersLimitExceededException::class.java)
    }

    @Test
    fun `subscribeBySharingCode - user subscribed on too many users`() {
        for (i in 2..10) {
            runBlocking { repository.save(Subscription(userId = "user$i", subscriberUserId = "user1")) }
        }
        runBlocking { assertThat(repository.count()).isEqualTo(9) }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user11",
            expiredAt = null
        )

        assertThatThrownBy { runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user1") } }
            .isExactlyInstanceOf(UserSubscriptionsLimitExceededException::class.java)
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

        assertThatThrownBy { runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2") } }
            .isEqualTo(UserBanedByOtherUserException(userId = "user2", bannedByUserId = "user1"))

        coVerify(exactly = 1) { userBanService.checkUserBanned("user2", "user1") }
    }

    @Test
    fun `subscribeBySharingCode - already exist`() {
        coEvery { accountSubscriptionService.getAccountSubscriptionByUserId(any()) } returns AccountSubscription(
            userId = "userId",
            subscriptionPlan = UserSubscriptionPlan.PRO
        )

        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        runBlocking { assertThat(repository.count()).isOne }

        coEvery { heartBeatSharingService.getSharingCodeByPublicCode("ABC123") } returns HeartBeatSharing(
            id = null,
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = null
        )

        val newSubscription = runBlocking { service.subscribeBySharingCode(code = "ABC123", userId = "user2") }
        assertThat(newSubscription).usingRecursiveComparison().ignoringFields("created").isEqualTo(subscription)

        runBlocking { assertThat(repository.count()).isOne }
    }

    @Test
    fun `unsubscribeFromUserById - without verification`() {
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
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
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
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
        val subscription = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        runBlocking { assertThat(repository.count()).isOne }

        assertThatThrownBy { runBlocking {
            service.unsubscribeFromUserById(subscription.id!!, validateUserId = "user1")
        } }.isEqualTo(SubscriptionNotFoundByIdException(id = subscription.id!!))

        runBlocking { assertThat(repository.count()).isOne }
    }

    @Test
    fun getSubscriptionById() {
        val expected = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }

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
        val expected1 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3")) }
        runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1")) } // not expected
        runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1")) } // not expected

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
        val expected1 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1")) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1")) }
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) } // not expected
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3")) } // not expected

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
            runBlocking { repository.save(Subscription(userId = "userId", subscriberUserId = "user$i")) }
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
            runBlocking { repository.save(Subscription(userId = "user$i", subscriberUserId = "userId")) }
        }
        runBlocking { assertThat(repository.count()).isEqualTo(10) }

        val result = runBlocking { service.checkUserHaveMaximumSubscriptions("userId") }
        assertThat(result).isTrue
    }

    @Test
    fun `checkUserHaveMaximumSubscriptions - false`() {
        val result = runBlocking { service.checkUserHaveMaximumSubscriptions("userId") }
        assertThat(result).isFalse
    }

    @Test
    fun handleUserDeletedEvent() {
        every { userBanService.handleUserDeletedEvent(any()) } returns Unit
        every { heartBeatSharingService.handleUserDeletedEvent(any()) } returns Unit
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1")) }
        val notDelete = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user3")) }

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
        runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user2")) }
        val expected1 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user1")) }
        val expected2 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user1")) }
        val expected3 = runBlocking { repository.save(Subscription(userId = "user1", subscriberUserId = "user3")) }
        val expected4 = runBlocking { repository.save(Subscription(userId = "user2", subscriberUserId = "user3")) }
        val expected5 = runBlocking { repository.save(Subscription(userId = "user3", subscriberUserId = "user2")) }
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