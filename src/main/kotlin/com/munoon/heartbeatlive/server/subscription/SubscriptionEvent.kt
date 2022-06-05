package com.munoon.heartbeatlive.server.subscription

sealed interface SubscriptionEvent {
    data class SubscriptionCreatedEvent(val subscription: Subscription): SubscriptionEvent
}