package com.munoon.heartbeatlive.server.user.controller

import com.munoon.heartbeatlive.server.DataLoaderNames
import com.munoon.heartbeatlive.server.user.User
import com.munoon.heartbeatlive.server.user.service.UserService
import graphql.GraphQLContext
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.dataloader.DataLoaderRegistry
import org.junit.jupiter.api.Test
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry

internal class UserBatchLoadingControllerTest {
    @Test
    fun `userById data loader`() {
        val userService = mockk<UserService>()

        val user1 = User(id = "user1", displayName = "Test1", email = null, emailVerified = false)
        val user2 = User(id = "user2", displayName = "Test2", email = null, emailVerified = false)
        every { userService.getUsersByIds(any()) } returns flowOf(user1, user2)

        val batchLoaderRegistry = DefaultBatchLoaderRegistry()
        UserBatchLoadingController(batchLoaderRegistry, userService)

        val dataLoaderRegistry = DataLoaderRegistry.newRegistry().build()
        batchLoaderRegistry.registerDataLoaders(dataLoaderRegistry, GraphQLContext.newContext().build())

        val dataLoader = dataLoaderRegistry.getDataLoader<String, User>(DataLoaderNames.USER_BY_ID_LOADER)

        val loadUser1 = dataLoader.load("user1")
        val loadUser2 = dataLoader.load("user2")

        val users = dataLoader.dispatchAndJoin()
        assertThat(users).usingRecursiveComparison().isEqualTo(listOf(user1, user2))

        assertThat(loadUser1.isDone).isTrue
        assertThat(loadUser1.get()).isEqualTo(user1)
        assertThat(loadUser2.isDone).isTrue
        assertThat(loadUser2.get()).isEqualTo(user2)

        verify(exactly = 1) { userService.getUsersByIds(setOf("user1", "user2")) }
    }
}