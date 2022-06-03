package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.heartrate.HeartRateMapper.asGraphQL
import com.munoon.heartbeatlive.server.heartrate.model.GraphqlHeartRateInfoTo
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HeartRateMapperTest {
    @Test
    fun asGraphQL() {
        val expected = GraphqlHeartRateInfoTo(
            subscriptionId = "subscription1",
            heartRate = 123.45f,
            ownHeartRate = true
        )

        val info = HeartRateInfo(
            subscriptionId = "subscription1",
            heartRate = 123.45f,
            ownHeartRate = true
        )

        assertThat(info.asGraphQL()).usingRecursiveComparison().isEqualTo(expected)
    }
}