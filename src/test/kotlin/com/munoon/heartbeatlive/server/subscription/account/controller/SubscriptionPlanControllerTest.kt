package com.munoon.heartbeatlive.server.subscription.account.controller

import com.munoon.heartbeatlive.server.AbstractGraphqlHttpTest
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.expectSingleError
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.isEqualsTo
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.language
import com.munoon.heartbeatlive.server.utils.GraphqlTestUtils.satisfyNoErrors
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.execution.ErrorType
import org.springframework.graphql.test.tester.GraphQlTester
import java.time.Duration

@SpringBootTest
class SubscriptionPlanControllerTest : AbstractGraphqlHttpTest() {
    @Test
    fun getSubscriptionPlans() {
        graphqlTester.language("ru-UA").document("""
            query {
                getSubscriptionPlans {
                    codeName,
                    displayName,
                    prices {
                        id,
                        price { amount, currency } ,
                        oldPrice { amount, currency } ,
                        duration
                    },
                    limits {
                        maxSharingCodesLimit,
                        maxSubscribersLimit,
                        maxSubscriptionsLimit
                    },
                    info {
                        descriptionItems
                    }
                }
            }
        """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionPlans[0].codeName").isEqualsTo("free")
            .path("getSubscriptionPlans[0].displayName").isEqualsTo("Бесплатная")
            .path("getSubscriptionPlans[0].prices").entityList(Any::class.java)
                .satisfies<GraphQlTester.EntityList<Any>> { it.shouldBeEmpty() }
            .path("getSubscriptionPlans[0].limits.maxSharingCodesLimit").isEqualsTo(2)
            .path("getSubscriptionPlans[0].limits.maxSubscribersLimit").isEqualsTo(1)
            .path("getSubscriptionPlans[0].limits.maxSubscriptionsLimit").isEqualsTo(1)
            .path("getSubscriptionPlans[0].info.descriptionItems").isEqualsTo(listOf(
                "Полностью бесплатно", "Одна бесплатная подписка", "Один бесплатный подписчик"))

            .path("getSubscriptionPlans[1].codeName").isEqualsTo("pro")
            .path("getSubscriptionPlans[1].displayName").isEqualsTo("Премиум")
            .path("getSubscriptionPlans[1].prices[0].id").entity(String::class.java).satisfies { it.shouldNotBeBlank() }
            .path("getSubscriptionPlans[1].prices[0].price.amount").isEqualsTo("5")
            .path("getSubscriptionPlans[1].prices[0].price.currency").isEqualsTo("USD")
            .path("getSubscriptionPlans[1].prices[0].oldPrice.amount").isEqualsTo("10")
            .path("getSubscriptionPlans[1].prices[0].oldPrice.currency").isEqualsTo("USD")
            .path("getSubscriptionPlans[1].prices[0].duration").isEqualsTo(Duration.ofDays(31).toSeconds())
            .path("getSubscriptionPlans[1].prices[1].id").entity(String::class.java).satisfies { it.shouldNotBeBlank() }
            .path("getSubscriptionPlans[1].prices[1].price.amount").isEqualsTo("2")
            .path("getSubscriptionPlans[1].prices[1].price.currency").isEqualsTo("USD")
            .path("getSubscriptionPlans[1].prices[1].oldPrice.amount").isEqualsTo("5")
            .path("getSubscriptionPlans[1].prices[1].oldPrice.currency").isEqualsTo("USD")
            .path("getSubscriptionPlans[1].prices[1].duration").isEqualsTo(Duration.ofDays(7).toSeconds())
            .path("getSubscriptionPlans[1].limits.maxSharingCodesLimit").isEqualsTo(20)
            .path("getSubscriptionPlans[1].limits.maxSubscribersLimit").isEqualsTo(30)
            .path("getSubscriptionPlans[1].limits.maxSubscriptionsLimit").isEqualsTo(30)
            .path("getSubscriptionPlans[1].info.descriptionItems").isEqualsTo(listOf(
                "До 30 подписчиков", "До 30 подписок", "Уведомления о совпадении сердцебиения"))
    }

    @Test
    fun getSubscriptionPlanByCodeName() {
        graphqlTester.language("ru-UA").document("""
            query {
                getSubscriptionPlanByCodeName(codeName: "pro") {
                    codeName,
                    displayName,
                    prices {
                        id,
                        price { amount, currency } ,
                        oldPrice { amount, currency } ,
                        duration
                    },
                    limits {
                        maxSharingCodesLimit,
                        maxSubscribersLimit,
                        maxSubscriptionsLimit
                    },
                    info {
                        descriptionItems
                    }
                }
            }
        """.trimIndent())
            .execute()
            .satisfyNoErrors()
            .path("getSubscriptionPlanByCodeName.codeName").isEqualsTo("pro")
            .path("getSubscriptionPlanByCodeName.displayName").isEqualsTo("Премиум")
            .path("getSubscriptionPlanByCodeName.prices[0].id").entity(String::class.java).satisfies { it.shouldNotBeBlank() }
            .path("getSubscriptionPlanByCodeName.prices[0].price.amount").isEqualsTo("5")
            .path("getSubscriptionPlanByCodeName.prices[0].price.currency").isEqualsTo("USD")
            .path("getSubscriptionPlanByCodeName.prices[0].oldPrice.amount").isEqualsTo("10")
            .path("getSubscriptionPlanByCodeName.prices[0].oldPrice.currency").isEqualsTo("USD")
            .path("getSubscriptionPlanByCodeName.prices[0].duration").isEqualsTo(Duration.ofDays(31).toSeconds())
            .path("getSubscriptionPlanByCodeName.prices[1].id").entity(String::class.java).satisfies { it.shouldNotBeBlank() }
            .path("getSubscriptionPlanByCodeName.prices[1].price.amount").isEqualsTo("2")
            .path("getSubscriptionPlanByCodeName.prices[1].price.currency").isEqualsTo("USD")
            .path("getSubscriptionPlanByCodeName.prices[1].oldPrice.amount").isEqualsTo("5")
            .path("getSubscriptionPlanByCodeName.prices[1].oldPrice.currency").isEqualsTo("USD")
            .path("getSubscriptionPlanByCodeName.prices[1].duration").isEqualsTo(Duration.ofDays(7).toSeconds())
            .path("getSubscriptionPlanByCodeName.limits.maxSharingCodesLimit").isEqualsTo(20)
            .path("getSubscriptionPlanByCodeName.limits.maxSubscribersLimit").isEqualsTo(30)
            .path("getSubscriptionPlanByCodeName.limits.maxSubscriptionsLimit").isEqualsTo(30)
            .path("getSubscriptionPlanByCodeName.info.descriptionItems").isEqualsTo(listOf(
                "До 30 подписчиков", "До 30 подписок", "Уведомления о совпадении сердцебиения"))
    }

    @Test
    fun `getSubscriptionPlanByCodeName - plan not found`() {
        graphqlTester.language("ru-UA").document("""
            query {
                getSubscriptionPlanByCodeName(codeName: "abc") {
                    codeName,
                    displayName,
                    prices {
                        id,
                        price { amount, currency } ,
                        oldPrice { amount, currency } ,
                        duration
                    },
                    limits {
                        maxSharingCodesLimit,
                        maxSubscribersLimit,
                        maxSubscriptionsLimit
                    },
                    info {
                        descriptionItems
                    }
                }
            }
        """.trimIndent())
            .execute()
            .errors().expectSingleError(
                errorType = ErrorType.NOT_FOUND,
                code = "account_subscription.subscription_plan.not_found.by_code_name",
                extensions = mapOf("name" to "abc"),
                path = "getSubscriptionPlanByCodeName"
            )
    }
}