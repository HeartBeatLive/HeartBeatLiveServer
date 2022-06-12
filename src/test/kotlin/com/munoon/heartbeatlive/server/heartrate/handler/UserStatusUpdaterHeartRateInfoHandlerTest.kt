package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class UserStatusUpdaterHeartRateInfoHandlerTest {
    private val userService = mockk<UserService>() {
        coEvery { writeUserHeartRate(any(), any(), any()) } returns User(
            id = "user1",
            displayName = null,
            email = null,
            emailVerified = false
        )
    }
    private val handler = UserStatusUpdaterHeartRateInfoHandler(userService)

    @Test
    fun handleHeartRateInfo() {
        runBlocking { handler.handleHeartRateInfo("user1", 123.45f) }
        coVerify(exactly = 1) {
            userService.writeUserHeartRate("user1", matchNullable { it != null }, any())
        }
    }
}