package com.munoon.heartbeatlive.server.user

import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.instant
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.mockk.MockKVerificationScope
import org.assertj.core.api.Assertions

object UserTestUtils {
    fun MockKVerificationScope.userArgumentMatch(expected: User) = match<User> {
        Assertions.assertThat(it).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        true
    }

    val userArbitrary = arbitrary { User(
        id = Arb.string(codepoints = Codepoint.alphanumeric(), size = 20).bind(),
        displayName = Arb.string(codepoints = Codepoint.alphanumeric(), range = 2..30).bind(),
        email = Arb.email().bind(),
        emailVerified = Arb.boolean().bind(),
        created = Arb.instant().bind(),
        roles = Arb.set(Arb.enum<UserRole>(), range = 0..UserRole.values().size).bind(),
        heartRates = Arb.list(heartRateArbitrary, range = 0..10).bind()
    ) }

    private val heartRateArbitrary = arbitrary { User.HeartRate(
        heartRate = Arb.int(range = 50..200).orNull().bind(),
        time = Arb.instant().bind()
    ) }
}