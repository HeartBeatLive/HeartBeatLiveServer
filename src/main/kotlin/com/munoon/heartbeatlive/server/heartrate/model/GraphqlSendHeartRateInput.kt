package com.munoon.heartbeatlive.server.heartrate.model

import javax.validation.constraints.Digits
import javax.validation.constraints.Max
import javax.validation.constraints.Positive

data class GraphqlSendHeartRateInput(
    @field:Positive
    @field:Max(500)
    @field:Digits(integer = 3, fraction = 2)
    val heartRate: Float
)