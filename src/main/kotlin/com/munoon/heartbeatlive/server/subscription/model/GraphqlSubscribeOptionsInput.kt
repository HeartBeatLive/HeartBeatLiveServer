package com.munoon.heartbeatlive.server.subscription.model

data class GraphqlSubscribeOptionsInput(
    val receiveHeartRateMatchNotifications: Boolean = false
)