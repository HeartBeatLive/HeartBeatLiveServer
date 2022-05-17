package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.DataLoaderNames
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.reactor.asFlux
import org.springframework.graphql.execution.BatchLoaderRegistry
import org.springframework.stereotype.Controller

@Controller
class UserBatchLoadingController(registry: BatchLoaderRegistry, service: UserService) {
    init {
        registry.forName<String, User>(DataLoaderNames.USER_BY_ID_LOADER).registerMappedBatchLoader { userIds, _ ->
            service.getUsersByIds(userIds).asFlux().collectMap { it.id }
        }
    }
}