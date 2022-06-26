package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.findSubscriptionPriceById
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionUtils.getActiveSubscriptionPlan
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

internal class AccountSubscriptionUtilsTest : FreeSpec({
    "getActiveSubscriptionPlan" - {
        "current subscription" {
            val expirationTime = Instant.now() + Duration.ofDays(10)
            val activeSubscription = getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
            assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.PRO)
        }

        "subscription expired" {
            val expirationTime = Instant.now() - Duration.ofDays(10)
            val activeSubscription = getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, expirationTime)
            assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
        }

        "expiration time not specified" {
            val activeSubscription = getActiveSubscriptionPlan(UserSubscriptionPlan.PRO, null)
            assertThat(activeSubscription).isEqualTo(UserSubscriptionPlan.FREE)
        }
    }

    "findAccountSubscriptionPlan" - {
        "found" {
            AccountSubscriptionUtils.findAccountSubscriptionPlan("pRO") shouldBe UserSubscriptionPlan.PRO
            AccountSubscriptionUtils.findAccountSubscriptionPlan("PRO") shouldBe UserSubscriptionPlan.PRO
            AccountSubscriptionUtils.findAccountSubscriptionPlan("pro") shouldBe UserSubscriptionPlan.PRO
        }

        "not found" {
            shouldThrowExactly<SubscriptionPlanNotFoundException> {
                AccountSubscriptionUtils.findAccountSubscriptionPlan("abc")
            } shouldBe SubscriptionPlanNotFoundException("abc")
        }
    }

    "findSubscriptionPriceById" - {
        "found" {
            val proSubscriptionPrice1 = SubscriptionProperties.SubscriptionPrice().apply {
                price = BigDecimal(30)
                duration = Duration.ofSeconds(30)
                currency = "USD"
            }
            val proSubscriptionPrice2 = SubscriptionProperties.SubscriptionPrice().apply {
                price = BigDecimal(40)
                duration = Duration.ofSeconds(60)
                currency = "USD"
            }
            val freeSubscriptionPrice1 = SubscriptionProperties.SubscriptionPrice().apply {
                price = BigDecimal(30)
                duration = Duration.ofSeconds(30)
                currency = "USD"
            }

            val properties = SubscriptionProperties().apply {
                subscription = mapOf(
                    UserSubscriptionPlan.PRO to SubscriptionProperties.Subscription().apply {
                        prices = listOf(proSubscriptionPrice1, proSubscriptionPrice2)
                    },
                    UserSubscriptionPlan.FREE to SubscriptionProperties.Subscription().apply {
                        prices = listOf(freeSubscriptionPrice1)
                    }
                )
            }

            val proSubscriptionPrice1Id = proSubscriptionPrice1.getId(UserSubscriptionPlan.PRO)
            properties.findSubscriptionPriceById(proSubscriptionPrice1Id) shouldBe proSubscriptionPrice1

            val proSubscriptionPrice2Id = proSubscriptionPrice2.getId(UserSubscriptionPlan.PRO)
            properties.findSubscriptionPriceById(proSubscriptionPrice2Id) shouldBe proSubscriptionPrice2

            val proSubscriptionPrice3Id = freeSubscriptionPrice1.getId(UserSubscriptionPlan.FREE)
            properties.findSubscriptionPriceById(proSubscriptionPrice3Id) shouldBe freeSubscriptionPrice1
        }

        "not found" {
            shouldThrowExactly<SubscriptionPlanPriceIsNotFoundByIdException> {
                SubscriptionProperties().findSubscriptionPriceById("abc")
            } shouldBe SubscriptionPlanPriceIsNotFoundByIdException("abc")
        }
    }
})