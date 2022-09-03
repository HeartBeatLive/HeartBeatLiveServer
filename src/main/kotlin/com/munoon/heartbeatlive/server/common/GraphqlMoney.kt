package com.munoon.heartbeatlive.server.common

import java.math.BigDecimal

data class GraphqlMoney(
    val amount: BigDecimal,
    val currency: String
)