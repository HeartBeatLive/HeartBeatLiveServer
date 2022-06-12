package com.munoon.heartbeatlive.server.heartrate.handler

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import com.munoon.heartbeatlive.server.heartrate.publisher.HeartRateMessagePublisher
import io.kotest.common.runBlocking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class MessagePublisherHeartRateInfoHandlerTest {
    private val publisher = mockk<HeartRateMessagePublisher>() {
        coEvery { publish(any()) } returns Unit
    }
    private val handler = MessagePublisherHeartRateInfoHandler(publisher)

    @Test
    fun handleHeartRateInfo() {
        runBlocking { handler.handleHeartRateInfo("user1", 123.45f) }

        coVerify(exactly = 1) { publisher.publish(match {
            it.heartRate == 123.45f && it.userId == "user1" && it.publisherId == ServerInstanceRunningId.id
        }) }
    }
}