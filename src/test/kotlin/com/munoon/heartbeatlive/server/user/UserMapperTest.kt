package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlProfile
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlPublicProfile
import com.munoon.heartbeatlive.server.user.UserMapper.asGraphqlSubscriptionUserProfile
import com.munoon.heartbeatlive.server.user.UserMapper.asNewUser
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlPublicProfileTo
import com.munoon.heartbeatlive.server.user.model.GraphqlSubscriptionUserProfileTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class UserMapperTest {
    @Test
    fun asGraphqlProfile() {
        val lastHeartRateInfoReceiveTime = Instant.now()
        val user = User(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(User.Role.ADMIN),
            heartRates = listOf(User.HeartRate(10, lastHeartRateInfoReceiveTime))
        )

        val expected = GraphqlProfileTo(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(User.Role.ADMIN),
            heartRates = listOf(User.HeartRate(10, lastHeartRateInfoReceiveTime))
        )

        val actual = user.asGraphqlProfile()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun asGraphqlPublicProfile() {
        val user = User(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(User.Role.ADMIN)
        )

        val expected = GraphqlPublicProfileTo(displayName = "Test Name")

        val actual = user.asGraphqlPublicProfile()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun asGraphqlSubscriptionUserProfile() {
        val lastHeartRateInfoReceiveTime = Instant.now()
        val user = User(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(User.Role.ADMIN),
            heartRates = listOf(User.HeartRate(10, lastHeartRateInfoReceiveTime))
        )

        val expected = GraphqlSubscriptionUserProfileTo(
            displayName = "Test Name",
            heartRates = listOf(User.HeartRate(10, lastHeartRateInfoReceiveTime))
        )

        val actual = user.asGraphqlSubscriptionUserProfile()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun asNewUser() {
        val expectedUser = User(
            id = "abc",
            displayName = null,
            email = "testemail@gmail.com",
            emailVerified = true
        )

        val createUserInput = GraphqlFirebaseCreateUserInput(
            id = "abc",
            email = "TESTEMAIL@gmail.com",
            emailVerified = true
        )

        val user = createUserInput.asNewUser()
        assertThat(user).usingRecursiveComparison().ignoringFields("created").isEqualTo(expectedUser)
    }
}