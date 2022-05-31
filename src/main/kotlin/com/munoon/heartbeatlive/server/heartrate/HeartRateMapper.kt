package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.heartrate.model.GraphqlHeartRateInfoTo
import com.munoon.heartbeatlive.server.heartrate.model.HeartRateInfo

object HeartRateMapper {
    fun HeartRateInfo.asGraphQL() = GraphqlHeartRateInfoTo(
        subscriptionId = subscriptionId,
        heartRate = heartRate,
        ownHeartRate = ownHeartRate
    )
}