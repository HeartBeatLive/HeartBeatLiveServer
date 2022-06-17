package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.DataLoaders
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.reactor.asFlux
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller

@Controller
class UserBatchLoader(registry: BatchLoaderRegistry, service: UserService) {
    init {
        DataLoaders.USER_BY_ID.register(registry).registerMappedBatchLoader { userIds, _ ->
            service.getUsersByIds(userIds).asFlux().collectMap { it.id }
        }
    }
}