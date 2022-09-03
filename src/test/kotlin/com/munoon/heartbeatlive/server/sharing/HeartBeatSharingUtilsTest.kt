package com.munoon.heartbeatlive.server.sharing

import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils.checkExpired
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils.checkUnlocked
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils.isExpired
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.OffsetDateTime

internal class HeartBeatSharingUtilsTest {
    @Test
    fun generatePublicCode() {
        val publicCodes = arrayListOf<String>()
        repeat(100) {
            val publicCode = HeartBeatSharingUtils.generatePublicCode()
            assertThat(publicCode.length).isEqualTo(6)
            assertThat(publicCodes).doesNotContain(publicCode)

            publicCodes += publicCode
        }
    }

    @Test
    fun `isExpired - true`() {
        val expiredAt = OffsetDateTime.now().minus(Duration.ofDays(5)).toInstant()
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = expiredAt)

        val result = sharing.isExpired()
        assertThat(result).isTrue
    }

    @Test
    fun `isExpired - false`() {
        val expiredAt = OffsetDateTime.now().plus(Duration.ofDays(5)).toInstant()
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = expiredAt)

        val result = sharing.isExpired()
        assertThat(result).isFalse
    }

    @Test
    fun `isExpired - null`() {
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = null)

        val result = sharing.isExpired()
        assertThat(result).isFalse
    }

    @Test
    fun `checkExpired - expired`() {
        val expiredAt = OffsetDateTime.now().minus(Duration.ofDays(5)).toInstant()
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = expiredAt)

        assertThatThrownBy { sharing.checkExpired() }
            .isExactlyInstanceOf(HeartBeatSharingExpiredException::class.java)
    }

    @Test
    fun `checkExpired - non expired`() {
        val expiredAt = OffsetDateTime.now().plus(Duration.ofDays(5)).toInstant()
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = expiredAt)

        assertThatNoException().isThrownBy { sharing.checkExpired() }
    }

    @Test
    fun `checkExpired - null`() {
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = null)

        assertThatNoException().isThrownBy { sharing.checkExpired() }
    }

    @Test
    fun `checkUnlocked - unlocked`() {
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = null,
            locked = false)

        shouldNotThrowAny { sharing.checkUnlocked() }
    }

    @Test
    fun `checkUnlocked - locked`() {
        val sharing = HeartBeatSharing(id = null, publicCode = "ABC", userId = "userId", expiredAt = null,
            locked = true)

        shouldThrowExactly<HeartBeatSharingLockedException> { sharing.checkUnlocked() }
    }
}