package com.munoon.heartbeatlive.server.config.properties

import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import java.math.BigDecimal
import java.time.Duration

class SubscriptionPropertiesTest : FreeSpec({
    "subscription price identifier" {
        val price1 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "USD"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId"
        }

        val price2 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "USD"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId"
        }

        val price3 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(30)
            currency = "USD"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId"
        }

        val price4 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "UAH"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId"
        }

        val price5 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "USD"
            duration = Duration.ofMinutes(60)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId"
        }

        val price6 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "USD"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(40)
            stripePriceId = "stripePriceId"
        }

        val price7 = SubscriptionProperties.SubscriptionPrice().apply {
            price = BigDecimal(20)
            currency = "USD"
            duration = Duration.ofMinutes(30)
            oldPrice = BigDecimal(30)
            stripePriceId = "stripePriceId2"
        }

        price1.getId(UserSubscriptionPlan.PRO) shouldMatch Regex("subpr_\\d*")

        price1.getId(UserSubscriptionPlan.PRO) shouldBe price2.getId(UserSubscriptionPlan.PRO)
        price1.getId(UserSubscriptionPlan.PRO) shouldNotBe price1.getId(UserSubscriptionPlan.FREE)
        price1.getId(UserSubscriptionPlan.PRO) shouldNotBe price3.getId(UserSubscriptionPlan.PRO)
        price1.getId(UserSubscriptionPlan.PRO) shouldNotBe price4.getId(UserSubscriptionPlan.PRO)
        price1.getId(UserSubscriptionPlan.PRO) shouldNotBe price5.getId(UserSubscriptionPlan.PRO)
        price1.getId(UserSubscriptionPlan.PRO) shouldBe price6.getId(UserSubscriptionPlan.PRO)
        price1.getId(UserSubscriptionPlan.PRO) shouldBe price7.getId(UserSubscriptionPlan.PRO)
    }
})
