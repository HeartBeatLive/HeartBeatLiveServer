package com.munoon.heartbeatlive.server.user

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
}