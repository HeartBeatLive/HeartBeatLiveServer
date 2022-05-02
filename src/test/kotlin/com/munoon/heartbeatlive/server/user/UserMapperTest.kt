package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.model.GraphqlProfileTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UserMapperTest {
    @Test
    fun asGraphqlProfile() {
        val user = User(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )

        val expected = GraphqlProfileTo(
            id = "1",
            displayName = "Test Name",
            email = "email@example.com",
            emailVerified = true,
            roles = setOf(UserRole.ADMIN)
        )

        val actual = user.asGraphqlProfile()
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