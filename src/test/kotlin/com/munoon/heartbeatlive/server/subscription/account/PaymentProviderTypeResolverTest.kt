package com.munoon.heartbeatlive.server.subscription.account

import com.munoon.heartbeatlive.server.subscription.account.model.GraphqlStripePaymentProvider
import graphql.execution.TypeResolutionParameters
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLSchema
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PaymentProviderTypeResolverTest : FreeSpec({
    val resolver = PaymentProviderTypeResolver()

    "getType" - {
        "Stripe Payment Provider" {
            val graphqlObject = GraphQLObjectType.newObject()
                .name("SampleObject")
                .build()

            val schema = mockk<GraphQLSchema>() {
                every { getObjectType(any()) } returns graphqlObject
            }

            val typeResolutionEnvironment = TypeResolutionParameters.newParameters()
                .schema(schema)
                .value(GraphqlStripePaymentProvider("publicApiKey"))
                .build()

            resolver.getType(typeResolutionEnvironment) shouldBe graphqlObject
            
            verify(exactly = 1) { schema.getObjectType("StripePaymentProvider") }
        }
    }
})
