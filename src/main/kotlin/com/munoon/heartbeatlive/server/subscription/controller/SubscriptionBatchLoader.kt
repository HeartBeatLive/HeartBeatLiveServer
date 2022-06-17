package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.DataLoaders
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import kotlinx.coroutines.reactor.asFlux
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller

@Controller
class SubscriptionBatchLoader(batchLoaderRegistry: BatchLoaderRegistry, subscriptionService: SubscriptionService) {
    init {
        DataLoaders.SUBSCRIPTION_BY_ID.register(batchLoaderRegistry).registerMappedBatchLoader { ids, _ ->
            subscriptionService.getAllByIds(ids).asFlux().collectMap { it.id!! }
        }
    }
}