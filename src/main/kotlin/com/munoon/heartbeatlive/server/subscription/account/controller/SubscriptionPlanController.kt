package com.munoon.heartbeatlive.server.subscription.account.controller

import com.google.common.base.Enums
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asGraphqlSubscription
import com.munoon.heartbeatlive.server.subscription.account.SubscriptionPlanNotFoundException
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlSubscriptionPlan
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class SubscriptionPlanController(
    private val subscriptionProperties: SubscriptionProperties
) {
    @QueryMapping
    fun getSubscriptionPlans(locale: Locale?): List<GraphqlSubscriptionPlan> {
        return subscriptionProperties.subscription.entries
            .map { (subscriptionPlan, subscription) -> subscription.asGraphqlSubscription(subscriptionPlan, locale) }
    }

    @QueryMapping
    fun getSubscriptionPlanByCodeName(@Argument codeName: String, locale: Locale?): GraphqlSubscriptionPlan {
        val subscriptionPlan = Enums.getIfPresent(UserSubscriptionPlan::class.java, codeName.uppercase()).orNull()
            ?: throw SubscriptionPlanNotFoundException(codeName)
        return subscriptionProperties[subscriptionPlan].asGraphqlSubscription(subscriptionPlan, locale)
    }
}