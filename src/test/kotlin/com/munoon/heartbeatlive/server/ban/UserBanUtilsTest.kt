package com.munoon.heartbeatlive.server.ban

import com.munoon.heartbeatlive.server.ban.UserBanUtils.validateUserBanned
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

internal class UserBanUtilsTest {
    @Test
    fun `validateUserBanned - valid`() {
        val service = mockk<UserBanService>() {
            coEvery { checkUserBanned(userId = "user2", bannedByUserId = "user1") } returns false
        }

        assertDoesNotThrow { runBlocking {
            service.validateUserBanned(userId = "user2", bannedByUserId = "user1")
        } }
    }

    @Test
    fun `validateUserBanned - invalid`() {
        val service = mockk<UserBanService>() {
            coEvery { checkUserBanned(userId = "user2", bannedByUserId = "user1") } returns true
        }

        assertThatThrownBy { runBlocking {
            service.validateUserBanned(userId = "user2", bannedByUserId = "user1")
        } }.isEqualTo(UserBanedByOtherUserException(userId = "user2", bannedByUserId = "user1"))
    }
}