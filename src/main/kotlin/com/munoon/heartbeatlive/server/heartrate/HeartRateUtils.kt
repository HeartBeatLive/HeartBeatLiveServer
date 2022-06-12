package com.munoon.heartbeatlive.server.heartrate

import kotlin.math.roundToInt

object HeartRateUtils {
    fun mapHeartRateToInteger(heartRate: Float): Int = heartRate.roundToInt()
}