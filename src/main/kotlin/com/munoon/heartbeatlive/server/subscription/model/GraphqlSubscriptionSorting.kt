package com.munoon.heartbeatlive.server.subscription.model

import org.springframework.data.domain.Sort

enum class GraphqlSubscriptionSorting {
    CREATED_ASC, CREATED_DESC
}

fun GraphqlSubscriptionSorting?.asSort() = when (this) {
    GraphqlSubscriptionSorting.CREATED_ASC -> Sort.by(Sort.Direction.ASC, "created")
    GraphqlSubscriptionSorting.CREATED_DESC -> Sort.by(Sort.Direction.DESC, "created")
    null -> Sort.unsorted()
}