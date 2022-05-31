package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.heartrate.HeartBeatSubscribersManager
import com.munoon.heartbeatlive.server.heartrate.LocalSubscribersSenderHeartRateInfoHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class LocalSubscribersSenderHeartRateInfoHandlerTest {
    private val subscribersManager = mockk<HeartBeatSubscribersManager>() {
        coEvery { sendHeartRate(any(), any()) } returns Unit
    }
    private val handler = LocalSubscribersSenderHeartRateInfoHandler(subscribersManager)

    @Test
    fun handleHeartRateInfo() {
        handler.handleHeartRateInfo("user1", 123.45f)
        coVerify(exactly = 1) { subscribersManager.sendHeartRate("user1", 123.45f) }
    }
}