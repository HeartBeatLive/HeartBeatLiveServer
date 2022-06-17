package com.munoon.heartbeatlive.server.heartrate.handler

interface HeartRateInfoHandler {
    suspend fun handleHeartRateInfo(userId: String, heartRate: Float)

    fun filter(userId: String, heartRate: Float): Boolean = true
}