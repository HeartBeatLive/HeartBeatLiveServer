package com.munoon.heartbeatlive.server.heartrate.model

data class HeartRateInfo(
    val subscriptionId: String?,
    val heartRate: Float,
    val ownHeartRate: Boolean
)