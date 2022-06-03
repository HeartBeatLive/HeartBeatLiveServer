package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.user.model.GraphqlUserHeartRateOnlineStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

internal class UserUtilsTest {
    private companion object {
        val heartRateStreamProperties = HeartRateStreamProperties().apply {
            leaveUserOnlineSinceLastHeartRateDuration = Duration.ofMinutes(2)
        }
    }

    @Test
    fun `getHeartRateOnlineStatus - ONLINE`() {
        val status = UserUtils.getHeartRateOnlineStatus(Instant.now(), heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.ONLINE)
    }

    @Test
    fun `getHeartRateOnlineStatus - OFFLINE because last receive time is null`() {
        val status = UserUtils.getHeartRateOnlineStatus(null, heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.OFFLINE)
    }

    @Test
    fun `getHeartRateOnlineStatus - OFFLINE because last receive time is far ago`() {
        val lastHeartRateInfoReceiveTime = OffsetDateTime.now().minusDays(2).toInstant()
        val status = UserUtils.getHeartRateOnlineStatus(lastHeartRateInfoReceiveTime, heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.OFFLINE)
    }
}