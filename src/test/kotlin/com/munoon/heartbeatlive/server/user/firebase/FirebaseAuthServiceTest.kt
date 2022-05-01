package com.munoon.heartbeatlive.server.user.firebase

import com.google.api.core.SettableApiFuture
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.munoon.heartbeatlive.server.auth.jwt.CustomJwtAuthenticationToken
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.UserRole
import io.mockk.MockKVerificationScope
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FirebaseAuthServiceTest {
    private val firebaseAuth = mockk<FirebaseAuth> {
        every { updateUserAsync(any()) }.returns(SettableApiFuture.create<UserRecord>().apply { set(mockk()) })
    }

    private val service = FirebaseAuthService(firebaseAuth)

    @Test
    fun updateFirebaseUser() {
        val expectedUpdateRecord = UserRecord.UpdateRequest("1")
            .setDisplayName("New Name")
            .setPhotoUrl(null)
            .setCustomClaims(mapOf( CustomJwtAuthenticationToken.ROLES_CLAIM to arrayListOf("ADMIN") ))

        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        val updatedUser = user.copy(displayName = "New Name", roles = setOf(UserRole.ADMIN))

        runBlocking { service.updateFirebaseUser(updatedUser, user) }
        coVerify(exactly = 1) { firebaseAuth.updateUserAsync(assertEq(expectedUpdateRecord)) }
    }

    @Test
    fun `updateFirebaseUser - no changes need`() {
        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        val updatedUser = user.copy(email = "newemail@example.com", emailVerified = false)

        runBlocking { service.updateFirebaseUser(updatedUser, user) }
        coVerify(exactly = 0) { firebaseAuth.updateUserAsync(any()) }
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
        runBlocking { service.initializeFirebaseUser(user) }
        coVerify(exactly = 1) { firebaseAuth.updateUserAsync(assertEq(expectedUpdateRecord)) }
    }

    @Test
    fun `initializeFirebaseUser - default user`() {
        val user = User(id = "1", displayName = null, email = "email@example.com", emailVerified = true)
        runBlocking { service.initializeFirebaseUser(user) }
        coVerify(exactly = 0) { firebaseAuth.updateUserAsync(any()) }
    }

    private fun MockKVerificationScope.assertEq(expected: UserRecord.UpdateRequest) = match<UserRecord.UpdateRequest> {
        assertThat(it).usingRecursiveComparison().isEqualTo(expected)
        true
    }
}