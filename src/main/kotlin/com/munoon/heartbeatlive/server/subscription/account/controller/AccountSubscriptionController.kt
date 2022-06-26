package com.munoon.heartbeatlive.server.subscription.account.controller

import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserId
import com.munoon.heartbeatlive.server.auth.utils.AuthUtils.authUserIdOrAnonymous
import com.munoon.heartbeatlive.server.subscription.account.AccountSubscriptionMapper.asGraphqlProviderInfo
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProvider
import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlPaymentProviderName
import com.munoon.heartbeatlive.server.subscription.account.service.AccountSubscriptionService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class AccountSubscriptionController(
    private val accountSubscriptionService: AccountSubscriptionService
) {
    private val logger = LoggerFactory.getLogger(AccountSubscriptionController::class.java)

    @QueryMapping
    suspend fun getPaymentProvider(@Argument supportedProviders: List<GraphqlPaymentProviderName>): GraphqlPaymentProvider {
        logger.info("User '${authUserIdOrAnonymous()}' request payment provider (supported providers: $supportedProviders)")
        return accountSubscriptionService.getPaymentProviderInfo(supportedProviders.toSet()).asGraphqlProviderInfo()
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    suspend fun stopRenewingSubscription(): Boolean {
        logger.info("User '${authUserId()}' stopped renewing his subscription")
        accountSubscriptionService.stopRenewingSubscription(authUserId())
        return true
    }
}