package com.munoon.heartbeatlive.server.user

import com.munoon.heartbeatlive.server.config.properties.HeartRateStreamProperties
import com.munoon.heartbeatlive.server.user.model.GraphqlUserHeartRateOnlineStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

internal class UserUtilsTest {
    private companion object {
        val heartRateStreamProperties = HeartRateStreamProperties().apply {
            this.storeUserHeartRateDuration = Duration.ofSeconds(30)
        }
    }

    @Test
    fun `getHeartRateOnlineStatus - ONLINE`() {
        val heartRates = listOf(
            User.HeartRate(50, Instant.now())
        )
        val status = UserUtils.getHeartRateOnlineStatus(heartRates, heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.ONLINE)
    }

    @Test
    fun `getHeartRateOnlineStatus - OFFLINE because last receive time is null`() {
        val heartRates = listOf(
            User.HeartRate(null, Instant.now()),
            User.HeartRate(50, Instant.now().minusSeconds(10))
        )
        val status = UserUtils.getHeartRateOnlineStatus(heartRates, heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.OFFLINE)
    }

    @Test
    fun `getHeartRateOnlineStatus - OFFLINE because last receive time is far ago`() {
        val heartRates = listOf(
            User.HeartRate(50, Instant.now().minusSeconds(60))
        )
        val status = UserUtils.getHeartRateOnlineStatus(heartRates, heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.OFFLINE)
    }

    @Test
    fun `getHeartRateOnlineStatus - OFFLINE because no heart rate received`() {
        val status = UserUtils.getHeartRateOnlineStatus(emptyList(), heartRateStreamProperties)
        assertThat(status).isEqualTo(GraphqlUserHeartRateOnlineStatus.OFFLINE)
    }
}