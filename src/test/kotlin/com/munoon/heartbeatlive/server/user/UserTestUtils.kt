package com.munoon.heartbeatlive.server.user

import io.mockk.MockKVerificationScope
import org.assertj.core.api.Assertions

object UserTestUtils {
    fun MockKVerificationScope.userArgumentMatch(expected: User) = match<User> {
        Assertions.assertThat(it).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        true
    }
}