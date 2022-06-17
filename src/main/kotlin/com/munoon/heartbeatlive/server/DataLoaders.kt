package com.munoon.heartbeatlive.server

import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.user.User
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.springframework.graphql.execution.BatchLoaderRegistry

object DataLoaders {
    val USER_BY_ID = LoaderDefinition<String, User>("userByIdLoader")
    val SUBSCRIPTION_BY_ID = LoaderDefinition<String, Subscription>("subscriptionByIdLoader")

    data class LoaderDefinition<K, V>(val name: String) {
        fun register(registry: BatchLoaderRegistry): BatchLoaderRegistry.RegistrationSpec<K, V> {
            return registry.forName(name)
        }

        operator fun get(dataLoaderRegistry: DataLoaderRegistry): DataLoader<K, V> {
            return dataLoaderRegistry.getDataLoader(name)
        }
    }
}