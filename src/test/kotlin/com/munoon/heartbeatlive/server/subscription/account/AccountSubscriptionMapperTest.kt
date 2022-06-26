package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.common.GraphqlMoney
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asGraphqlProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asGraphqlSubscription
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asSubscriptionJwt
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlStripePaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.StripePaymentProviderInfo
import com.munoon.heartbeatlive.server.user.User
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.instant
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.math.BigDecimal
import java.time.Duration
import java.util.*

class AccountSubscriptionMapperTest : FreeSpec({
    "asGraphqlProviderInfo" - {
        "Stripe Payment Info" {
            checkAll(Arb.string()) { stripePublicKey ->
                val expected = GraphqlStripePaymentProvider(stripePublicKey)
                val providerInfo = StripePaymentProviderInfo(stripePublicKey)
                providerInfo.asGraphqlProviderInfo() shouldBe expected
            }
        }
    }

    "asGraphqlSubscription" - {
        val subscription = SubscriptionProperties.Subscription().apply {
            displayName = mapOf(
                Locale.ENGLISH to "Premium",
                Locale("ru") to "Премиум"
            )
            prices = listOf(
                SubscriptionProperties.SubscriptionPrice().apply {
                    price = BigDecimal(5)
                    currency = "USD"
                    duration = Duration.ofDays(31)
                    oldPrice = BigDecimal(10)
                },
                SubscriptionProperties.SubscriptionPrice().apply {
                    price = BigDecimal(8)
                    currency = "USD"
                    duration = Duration.ofDays(62)
                }
            )
            limits = SubscriptionProperties.SubscriptionLimits().apply {
                maxSharingCodesLimit = 30
                maxSubscribersLimit = 20
                maxSubscriptionsLimit = 10
            }
            info = SubscriptionProperties.SubscriptionInfo().apply {
                descriptionItems = mapOf(
                    Locale.ENGLISH to listOf("Description 1", "Description 2"),
                    Locale("ru") to listOf("Описание")
                )
            }
        }

        "default locale" {
            val expected = GraphqlSubscriptionPlan(
                codeName = "pro",
                displayName = "Premium",
                prices = listOf(
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[0].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(5), "USD"),
                        oldPrice = GraphqlMoney(BigDecimal(10), "USD"),
                        duration = Duration.ofDays(31)
                    ),
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[1].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(8), "USD"),
                        oldPrice = null,
                        duration = Duration.ofDays(62)
                    )
                ),
                limits = GraphqlSubscriptionPlan.Limits(
                    maxSharingCodesLimit = 30,
                    maxSubscribersLimit = 20,
                    maxSubscriptionsLimit = 10
                ),
                info = GraphqlSubscriptionPlan.Info(
                    descriptionItems = listOf("Description 1", "Description 2")
                )
            )

            val actual = subscription.asGraphqlSubscription(UserSubscriptionPlan.PRO, null)
            actual shouldBe expected
        }

        "specified locale" {
            val expected = GraphqlSubscriptionPlan(
                codeName = "pro",
                displayName = "Премиум",
                prices = listOf(
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[0].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(5), "USD"),
                        oldPrice = GraphqlMoney(BigDecimal(10), "USD"),
                        duration = Duration.ofDays(31)
                    ),
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[1].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(8), "USD"),
                        oldPrice = null,
                        duration = Duration.ofDays(62)
                    )
                ),
                limits = GraphqlSubscriptionPlan.Limits(
                    maxSharingCodesLimit = 30,
                    maxSubscribersLimit = 20,
                    maxSubscriptionsLimit = 10
                ),
                info = GraphqlSubscriptionPlan.Info(
                    descriptionItems = listOf("Описание")
                )
            )

            val locale = Locale.forLanguageTag("ru-UA")
            val actual = subscription.asGraphqlSubscription(UserSubscriptionPlan.PRO, locale)
            actual shouldBe expected
        }

        "specified unknown locale" {
            val expected = GraphqlSubscriptionPlan(
                codeName = "pro",
                displayName = "Premium",
                prices = listOf(
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[0].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(5), "USD"),
                        oldPrice = GraphqlMoney(BigDecimal(10), "USD"),
                        duration = Duration.ofDays(31)
                    ),
                    GraphqlSubscriptionPlan.Price(
                        id = subscription.prices[1].getId(UserSubscriptionPlan.PRO),
                        price = GraphqlMoney(BigDecimal(8), "USD"),
                        oldPrice = null,
                        duration = Duration.ofDays(62)
                    )
                ),
                limits = GraphqlSubscriptionPlan.Limits(
                    maxSharingCodesLimit = 30,
                    maxSubscribersLimit = 20,
                    maxSubscriptionsLimit = 10
                ),
                info = GraphqlSubscriptionPlan.Info(
                    descriptionItems = listOf("Description 1", "Description 2")
                )
            )

            val actual = subscription.asGraphqlSubscription(UserSubscriptionPlan.PRO, Locale.FRENCH)
            actual shouldBe expected
        }
    }

    "asSubscriptionJwt" - {
        "with specified subscription" {
            checkAll(Arb.enum<UserSubscriptionPlan>(), Arb.instant()) { plan, expiresAt ->
                val expected = JwtUserSubscription(plan, expiresAt)
                val user = User(id = "userId", displayName = null, email = null, emailVerified = false,
                    subscription = User.Subscription(plan, expiresAt))
                user.asSubscriptionJwt() shouldBe expected
            }
        }

        "user subscription is null" {
            val user = User(id = "userId", displayName = null, email = null, emailVerified = false,
                subscription = null)
            user.asSubscriptionJwt() shouldBe JwtUserSubscription.DEFAULT
        }
    }
})