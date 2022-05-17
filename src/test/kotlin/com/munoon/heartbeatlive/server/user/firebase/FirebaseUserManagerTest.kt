package com.munoon.heartbeatlive.server.user.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.UserRole
import com.ninjasquad.springmockk.MockkBean
import io.mockk.MockKVerificationScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher

@SpringBootTest
internal class FirebaseUserManagerTest : AbstractTest() {
    @MockkBean
    private lateinit var firebaseAuth: FirebaseAuth

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @BeforeEach
    fun setUpFirebaseAuth() {
        firebaseAuth.apply {
            every { updateUser(any()) } returns mockk()
            every { deleteUser(any()) } returns mockk()
        }
    }

    @Test
    fun updateFirebaseUser() {
        val expectedUpdateRecord = UserRecord.UpdateRequest("1")
            .setDisplayName("New Name")
            .setPhotoUrl(null)
            .setCustomClaims(mapOf( CustomJwtAuthenticationToken.ROLES_CLAIM to arrayListOf("ADMIN") ))

        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        val updatedUser = user.copy(displayName = "New Name", roles = setOf(UserRole.ADMIN))

        val event = UserEvents.UserUpdatedEvent(newUser = updatedUser, oldUser = user, updateFirebaseState = true)
        eventPublisher.publishEvent(event)

        verify(exactly = 1) { firebaseAuth.updateUser(assertEq(expectedUpdateRecord)) }
    }

    @Test
    fun `updateFirebaseUser - ignore`() {
        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        val updatedUser = user.copy(displayName = "New Name", roles = setOf(UserRole.ADMIN))

        val event = UserEvents.UserUpdatedEvent(newUser = updatedUser, oldUser = user, updateFirebaseState = false)
        eventPublisher.publishEvent(event)

        verify(exactly = 0) { firebaseAuth.updateUser(any()) }
    }

    @Test
    fun `updateFirebaseUser - no changes need`() {
        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        val updatedUser = user.copy(email = "newemail@example.com", emailVerified = false)

        val event = UserEvents.UserUpdatedEvent(newUser = updatedUser, oldUser = user, updateFirebaseState = true)
        eventPublisher.publishEvent(event)

        verify(exactly = 0) { firebaseAuth.updateUser(any()) }
    }

    @Test
    fun initializeFirebaseUser() {
        val expectedUpdateRecord = UserRecord.UpdateRequest("1")
            .setDisplayName("Display Name")
            .setPhotoUrl(null)
            .setCustomClaims(mapOf( CustomJwtAuthenticationToken.ROLES_CLAIM to arrayListOf("ADMIN") ))

        val user = User(
            id = "1",
            displayName = "Display Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )

        eventPublisher.publishEvent(UserEvents.UserCreatedEvent(user))

        verify(exactly = 1) { firebaseAuth.updateUser(assertEq(expectedUpdateRecord)) }
    }

    @Test
    fun `initializeFirebaseUser - default user`() {
        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        eventPublisher.publishEvent(UserEvents.UserCreatedEvent(user))
        verify(exactly = 0) { firebaseAuth.updateUser(any()) }
    }

    @Test
    fun deleteFirebaseUser() {
        val userId = "abc"
        eventPublisher.publishEvent(UserEvents.UserDeletedEvent(userId, updateFirebaseState = true))
        verify(exactly = 1) { firebaseAuth.deleteUser(userId) }
    }

    @Test
    fun `deleteFirebaseUser - ignore`() {
        val userId = "abc"
        eventPublisher.publishEvent(UserEvents.UserDeletedEvent(userId, updateFirebaseState = false))
        verify(exactly = 0) { firebaseAuth.deleteUser(userId) }
    }

    private fun MockKVerificationScope.assertEq(expected: UserRecord.UpdateRequest) = match<UserRecord.UpdateRequest> {
        assertThat(it).usingRecursiveComparison().isEqualTo(expected)
        true
    }
}