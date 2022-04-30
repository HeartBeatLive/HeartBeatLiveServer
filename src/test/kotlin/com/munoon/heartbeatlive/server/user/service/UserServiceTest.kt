package com.munoon.heartbeatlive.server.user.service

import com.munoon.heartbeatlive.server.AbstractMongoDBTest
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserNotFoundByIdException
import com.munoon.heartbeatlive.server.user.UserTestUtils.userArgumentMatch
import com.munoon.heartbeatlive.server.user.firebase.FirebaseAuthService
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.UpdateUserInfoFromJwtTo
import com.munoon.heartbeatlive.server.user.repository.UserRepository
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coVerify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class UserServiceTest : AbstractMongoDBTest() {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockkBean(relaxed = true)
    private lateinit var firebaseAuthService: FirebaseAuthService

    @Test
    fun `checkEmailReserved - true`() {
        val email = "testemail@gmail.com"
        runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(id = "1", email, emailVerified = true)) }
        val resp = runBlocking { userService.checkEmailReserved(email) }
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
            assertThat(userRepository.count().awaitSingle()).isZero
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "1", email = "testemail@gmail.com", emailVerified = true))

            assertThat(userRepository.findAll().asFlow().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))
            coVerify(exactly = 1) { firebaseAuthService.initializeFirebaseUser(userArgumentMatch(expectedUser)) }
        }
    }

    @Test
    fun deleteUserByIdFirebaseTrigger() {
        runBlocking {
            val userId = "1"
            userService.createUser(GraphqlFirebaseCreateUserInput(id = userId, email = "testemail@gmail.com", emailVerified = true))
            userService.deleteUserByIdFirebaseTrigger(userId)
            assertThat(userRepository.count().awaitSingle()).isZero
        }
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
            userService.createUser(GraphqlFirebaseCreateUserInput(id = userId, email = "testemail@gmail.com", emailVerified = true))
            val updatedUser = userService.updateUserDisplayName(userId, "Test Name")

            assertThat(updatedUser).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
            assertThat(userRepository.findAll().asFlow().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))
            coVerify(exactly = 1) {
                firebaseAuthService.updateFirebaseUser(
                    newUser = userArgumentMatch(expectedUser),
                    oldUser = userArgumentMatch(expectedUser.copy(displayName = null))
                )
            }
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

        runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(id = userId, email = "testemail@gmail.com", emailVerified = false)) }

        val updatedUser = runBlocking { userService.updateUserInfoFromJwt(userId, UpdateUserInfoFromJwtTo(emailVerified = true)) }
        assertThat(updatedUser).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
        runBlocking {
            assertThat(userRepository.findAll().asFlow().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created")
                .isEqualTo(listOf(expectedUser))
        }
    }

    @Test
    fun `updateUserInfoFromJwt - user not found`() {
        assertThatThrownBy {
            runBlocking { userService.updateUserInfoFromJwt("abc", UpdateUserInfoFromJwtTo(emailVerified = true)) }
        }.isEqualTo(UserNotFoundByIdException("abc"))
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
        runBlocking {
            userService.createUser(GraphqlFirebaseCreateUserInput(id = "1", email = "testemail@gmail.com", emailVerified = true))
        }

        val user = runBlocking { userService.getUserById("1") }
        assertThat(user).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
    }

    @Test
    fun `getUserById - not found`() {
        assertThatThrownBy { runBlocking { userService.getUserById("abc") } }
            .isEqualTo(UserNotFoundByIdException("abc"))
    }
}