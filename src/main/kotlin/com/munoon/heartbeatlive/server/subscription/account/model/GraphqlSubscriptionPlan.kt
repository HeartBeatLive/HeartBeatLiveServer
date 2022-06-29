package com.munoon.heartbeatlive.server.subscription.account.model

import com.munoon.heartbeatlive.server.common.GraphqlMoney
import java.time.Duration

data class GraphqlSubscriptionPlan(
    val codeName: String,
    val displayName: String,
    val prices: List<Price>,
    val limits: Limits,
    val info: Info
) {
    data class Limits(
        val maxSharingCodesLimit: Int,
        val maxSubscribersLimit: Int,
        val maxSubscriptionsLimit: Int
    )

    data class Info(
        val descriptionItems: List<String>
    )

    data class Price(
        val id: String,
        val price: GraphqlMoney,
        val oldPrice: GraphqlMoney?,
        val duration: Duration,
        val refundDuration: Duration
    )
}