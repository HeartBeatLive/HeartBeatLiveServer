package com.munoon.heartbeatlive.server.user.service

import com.munoon.heartbeatlive.server.AbstractMongoDBTest
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import java.time.Duration
import java.time.Instant

@SpringBootTest
@RecordApplicationEvents
class UserServiceTest : AbstractMongoDBTest() {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var events: ApplicationEvents

    @Test
    fun `checkEmailReserved - true`() {
        val createUserInput = GraphqlFirebaseCreateUserInput(
            id = "1",
            email = "testemail@gmail.com",
            emailVerified = true
        )
        runBlocking { userService.createUser(createUserInput) }

        val resp = runBlocking { userService.checkEmailReserved("TESTEMAIL@gmail.com") }
        assertThat(resp).isTrue
    }

    @Test
    fun `checkEmailReserved - false`() {
        val resp = runBlocking { userService.checkEmailReserved("testemail@gmail.com") }
        assertThat(resp).isFalse
    }

    @Test
    fun createUser() {
        val expectedUser = User(
            id = "1",
            displayName = null,
            email = "testemail@gmail.com",
            emailVerified = true,
            created = Instant.now(),
            roles = emptySet()
        )

        runBlocking {
            assertThat(userRepository.count()).isZero

            val request = GraphqlFirebaseCreateUserInput(id = "1", email = "TESTEMAIL@gmail.com", emailVerified = true)
            val user = userService.createUser(request)

            assertThat(userRepository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))

            val expectedEvent = UserEvents.UserCreatedEvent(user)
            assertThat(events.stream(UserEvents.UserCreatedEvent::class.java))
                .usingRecursiveComparison()
                .isEqualTo(listOf(expectedEvent))
        }
    }

    @Test
    fun `deleteUserById with firebase state updating`() {
        val userId = "1"
        runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(
                id = userId,
                email = "testemail@gmail.com",
                emailVerified = true
            ))
            userService.deleteUserById(userId, updateFirebaseState = true)
            assertThat(userRepository.count()).isZero
        }

        val expectedEvent = UserEvents.UserDeletedEvent(userId, updateFirebaseState = true)
        assertThat(events.stream(UserEvents.UserDeletedEvent::class.java))
            .usingRecursiveComparison()
            .isEqualTo(listOf(expectedEvent))
    }

    @Test
    fun `deleteUserById without firebase state updating`() {
        val userId = "1"
        runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(
                id = userId,
                email = "testemail@gmail.com",
                emailVerified = true
            ))
            userService.deleteUserById(userId, updateFirebaseState = false)
            assertThat(userRepository.count()).isZero
        }

        val expectedEvent = UserEvents.UserDeletedEvent(userId, updateFirebaseState = false)
        assertThat(events.stream(UserEvents.UserDeletedEvent::class.java))
            .usingRecursiveComparison()
            .isEqualTo(listOf(expectedEvent))
    }

    @Test
    fun `deleteUserById user not found`() {
        assertThatThrownBy { runBlocking { userService.deleteUserById("abc", updateFirebaseState = true) } }
            .isEqualTo(UserNotFoundByIdException("abc"))

        assertThat(events.stream(UserEvents.UserDeletedEvent::class.java)).isEmpty()
    }

    @Test
    fun updateUserDisplayName() {
        val expectedUser = User(
            id = "1",
            displayName = "Test Name",
            email = "testemail@gmail.com",
            emailVerified = true,
            created = Instant.now(),
            roles = emptySet()
        )

        val userId = "1"
        runBlocking {
            val oldUser = userService.createUser(GraphqlFirebaseCreateUserInput(
                id = userId,
                email = "testemail@gmail.com",
                emailVerified = true
            ))
            val updatedUser = userService.updateUserDisplayName(userId, "Test Name")

            assertThat(updatedUser).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
            assertThat(userRepository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))

            val expectedEvent = UserEvents.UserUpdatedEvent(
                newUser = updatedUser,
                oldUser = oldUser,
                updateFirebaseState = true
            )
            assertThat(events.stream(UserEvents.UserUpdatedEvent::class.java))
                .usingRecursiveComparison()
                .ignoringFields("oldUser.created", "newUser.created")
                .isEqualTo(listOf(expectedEvent))
        }
    }

    @Test
    fun `updateUserDisplayName - user not found`() {
        assertThatThrownBy {
            runBlocking { userService.updateUserDisplayName("abc", "Test Name") }
        }.isEqualTo(UserNotFoundByIdException("abc"))
    }

    @Test
    fun updateUserInfoFromJwt() {
        val userId = "1"
        val expectedUser = User(
            id = userId,
            displayName = null,
            email = "testemail@gmail.com",
            emailVerified = true
        )

        val oldUser = runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(
            id = userId,
            email = "testemail@gmail.com",
            emailVerified = false
        )) }

        val updateUserInfo = UpdateUserInfoFromJwtTo(emailVerified = true)
        val updatedUser = runBlocking { userService.updateUserInfoFromJwt(userId, updateUserInfo) }
        assertThat(updatedUser).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
        runBlocking {
            assertThat(userRepository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))
        }

        val expectedEvent = UserEvents.UserUpdatedEvent(
            newUser = updatedUser,
            oldUser = oldUser,
            updateFirebaseState = false
        )
        assertThat(events.stream(UserEvents.UserUpdatedEvent::class.java))
            .usingRecursiveComparison()
            .ignoringFields("oldUser.created", "newUser.created")
            .isEqualTo(listOf(expectedEvent))
    }

    @Test
    fun `updateUserInfoFromJwt - user not found`() {
        val updateUserInfo = UpdateUserInfoFromJwtTo(emailVerified = true)
        assertThatThrownBy {
            runBlocking { userService.updateUserInfoFromJwt("abc", updateUserInfo) }
        }.isEqualTo(UserNotFoundByIdException("abc"))
    }

    @Test
    fun updateUserSubscription() {
        val userId = "1"
        val expiresAt = Instant.now().plusSeconds(60)
        val startAt = Instant.now().let { Instant.ofEpochSecond(it.epochSecond) }
        val expectedUser = User(
            id = userId,
            displayName = null,
            email = "testemail@gmail.com",
            emailVerified = false,
            subscription = User.Subscription(
                plan = UserSubscriptionPlan.PRO,
                expiresAt = expiresAt,
                startAt = startAt,
                refundDuration = Duration.ofDays(3),
                details = User.Subscription.StripeSubscriptionDetails(
                    subscriptionId = "stripeSubscription1",
                    paymentIntentId = "stripePaymentIntent1"
                )
            )
        )

        val oldUser = runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(
            id = userId,
            email = "testemail@gmail.com",
            emailVerified = false
        )) }

        val newUserSubscription = User.Subscription(
            plan = UserSubscriptionPlan.PRO,
            expiresAt = expiresAt,
            startAt = startAt,
            refundDuration = Duration.ofDays(3),
            details = User.Subscription.StripeSubscriptionDetails(
                subscriptionId = "stripeSubscription1",
                paymentIntentId = "stripePaymentIntent1"
            )
        )
        val updatedUser = runBlocking { userService.updateUserSubscription(userId, newUserSubscription) }
        assertThat(updatedUser).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
        runBlocking {
            assertThat(userRepository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created", "subscription.expiresAt")
                .isEqualTo(listOf(expectedUser))
        }

        val expectedEvent = UserEvents.UserUpdatedEvent(
            newUser = updatedUser,
            oldUser = oldUser,
            updateFirebaseState = true
        )
        assertThat(events.stream(UserEvents.UserUpdatedEvent::class.java))
            .usingRecursiveComparison()
            .ignoringFields("oldUser.created", "newUser.created")
            .isEqualTo(listOf(expectedEvent))
    }

    @Test
    fun `updateUserSubscription - not found`() {
        val newUserSubscription = User.Subscription(
            plan = UserSubscriptionPlan.PRO,
            expiresAt = Instant.now(),
            startAt = Instant.now(),
            refundDuration = Duration.ofDays(3),
            details = User.Subscription.StripeSubscriptionDetails(
                subscriptionId = "stripeSubscription1",
                paymentIntentId = "stripePaymentIntent1"
            )
        )
        shouldThrowExactly<UserNotFoundByIdException> {
            runBlocking { userService.updateUserSubscription("abc", newUserSubscription) }
        } shouldBe UserNotFoundByIdException("abc")
    }

    @Test
    fun getUserById() {
        val expectedUser = User(
            id = "1",
            displayName = null,
            email = "testemail@gmail.com",
            emailVerified = true,
            created = Instant.now(),
            roles = emptySet()
        )

        val createUserRequest = GraphqlFirebaseCreateUserInput(
            id = "1",
            email = "testemail@gmail.com",
            emailVerified = true
        )
        runBlocking { userService.createUser(createUserRequest) }

        val user = runBlocking { userService.getUserById("1") }
        assertThat(user).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
    }

    @Test
    fun `getUserById - not found`() {
        assertThatThrownBy { runBlocking { userService.getUserById("abc") } }
            .isEqualTo(UserNotFoundByIdException("abc"))
    }

    @Test
    fun getUsersByIds() {
        val user1 = runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "user1", email = null, emailVerified = false))
        }
        val user2 = runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "user2", email = null, emailVerified = false))
        }
        runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "user3", email = null, emailVerified = false))
        }

        runBlocking {
            assertThat(userService.getUsersByIds(setOf("user1", "user2")).toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(user1, user2))
        }
    }

    @Test
    fun writeUserHeartRate() {
        val heartRate1Time = Instant.now()
        val heartRate2Time = Instant.now()

        val expected = User(
            id = "user1",
            displayName = "Display Name",
            email = null,
            emailVerified = false,
            heartRates = listOf(
                User.HeartRate(null, heartRate2Time),
                User.HeartRate(21, heartRate1Time)
            )
        )

        runBlocking { userRepository.save(User(
            id = "user1",
            displayName = "Display Name",
            email = null,
            emailVerified = false
        )) }

        runBlocking { userService.writeUserHeartRate("user1", 20.6f, Instant.now()) }
        runBlocking { userService.writeUserHeartRate("user1", null, Instant.now()) }

        runBlocking {
            assertThat(userRepository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created", "heartRates.time")
                .isEqualTo(listOf(expected))
        }

        val expectedEvent1 = UserEvents.UserUpdatedEvent(
            newUser = expected.copy(heartRates = listOf(
                User.HeartRate(21, heartRate1Time)
            )),
            oldUser = expected.copy(heartRates = emptyList()),
            updateFirebaseState = false
        )
        val expectedEvent2 = UserEvents.UserUpdatedEvent(
            newUser = expected.copy(heartRates = listOf(
                User.HeartRate(null, heartRate2Time),
                User.HeartRate(21, heartRate1Time)
            )),
            oldUser = expected.copy(heartRates = listOf(
                User.HeartRate(21, heartRate1Time)
            )),
            updateFirebaseState = false
        )
        assertThat(events.stream(UserEvents.UserUpdatedEvent::class.java))
            .usingRecursiveComparison()
            .ignoringFields("oldUser.created", "newUser.created", "newUser.heartRates.time", "oldUser.heartRates.time")
            .isEqualTo(listOf(expectedEvent1, expectedEvent2))
    }

    @Test
    fun `writeUserHeartRate - user not found`() {
        assertThatThrownBy { runBlocking {
            userService.writeUserHeartRate("abc", null, Instant.now())
        } }.isEqualTo(UserNotFoundByIdException("abc"))
    }
}