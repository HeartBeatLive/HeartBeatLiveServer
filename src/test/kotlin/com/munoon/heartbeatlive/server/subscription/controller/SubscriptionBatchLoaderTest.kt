package com.munoon.heartbeatlive.server.subscription.controller

import com.munoon.heartbeatlive.server.DataLoaders
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.service.SubscriptionService
import graphql.GraphQLContext
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.future.shouldBeCompleted
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.asFlow
import org.dataloader.DataLoaderRegistry
import org.springframework.graphql.execution.DefaultBatchLoaderRegistry

class SubscriptionBatchLoaderTest : FreeSpec({
    "subscription batch loader" {
        val subscriptionArbitrary = arbitrary { Subscription(
            id = Arb.string(codepoints = Codepoint.alphanumeric()).bind(),
            userId = Arb.string(codepoints = Codepoint.alphanumeric()).bind(),
            subscriberUserId = Arb.string(codepoints = Codepoint.alphanumeric()).bind(),
            receiveHeartRateMatchNotifications = Arb.boolean().bind()
        ) }

        checkAll(5, subscriptionArbitrary, subscriptionArbitrary) { subscription1, subscription2 ->
            val service = mockk<SubscriptionService>() {
                every { getAllByIds(any()) } returns listOf(subscription1, subscription2).asFlow()
            }

            val batchLoaderRegistry = DefaultBatchLoaderRegistry()
            SubscriptionBatchLoader(batchLoaderRegistry, service)

            val dataLoaderRegistry = DataLoaderRegistry.newRegistry().build()
            batchLoaderRegistry.registerDataLoaders(dataLoaderRegistry, GraphQLContext.newContext().build())

            val dataLoader = DataLoaders.SUBSCRIPTION_BY_ID[dataLoaderRegistry]

            val subscription1Load = dataLoader.load(subscription1.id)
            val subscription2Load = dataLoader.load(subscription2.id)

            dataLoader.dispatchAndJoin()

            subscription1Load.shouldBeCompleted()
            subscription1Load.get() shouldBe subscription1
            subscription2Load.shouldBeCompleted()
            subscription2Load.get() shouldBe subscription2

            verify(exactly = 1) { service.getAllByIds(setOf(subscription1.id!!, subscription2.id!!)) }
        }
    }
})
