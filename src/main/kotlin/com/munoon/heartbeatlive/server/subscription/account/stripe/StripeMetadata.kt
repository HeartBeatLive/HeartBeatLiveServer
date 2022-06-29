package com.munoon.heartbeatlive.server.subscription.account.stripe

import com.google.common.base.Enums
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import java.time.Duration
import kotlin.reflect.KClass

object StripeMetadata {
    object Subscription {
        val REFUND_DURATION = DurationMetadata("refundSeconds")
        val SUBSCRIPTION_PLAN = EnumMetadata("subscriptionPlan", UserSubscriptionPlan::class)
        val USER_ID = StringMetadata("uid")
    }

    object Customer {
        val USER_ID = StringMetadata("uid")
    }

    abstract class StripeMetadata<T>(val key: String) {
        fun getValue(map: Map<String, String>): T? = map[key]?.let { convertToType(it) }
        fun addValue(value: T): Pair<String, String> = key to convertToString(value)

        protected abstract fun convertToType(value: String): T?
        protected abstract fun convertToString(value: T): String
    }

    class DurationMetadata(key: String) : StripeMetadata<Duration>(key) {
        override fun convertToType(value: String) = value.toLongOrNull()?.let { Duration.ofSeconds(it) }
        override fun convertToString(value: Duration) = value.toSeconds().toString()
    }

    class StringMetadata(key: String) : StripeMetadata<String>(key) {
        override fun convertToType(value: String) = value
        override fun convertToString(value: String) = value
    }

    class EnumMetadata<T : Enum<T>>(key: String, private val enumClass: KClass<T>) : StripeMetadata<T>(key) {
        override fun convertToType(value: String): T? = Enums.getIfPresent(enumClass.java, value.uppercase()).orNull()
        override fun convertToString(value: T): String = value.name
    }
}