package com.munoon.heartbeatlive.server.heartrate.model

data class GraphqlHeartRateInfoTo(
    val subscriptionId: String?,
    val heartRate: Float,
    val ownHeartRate: Boolean
)