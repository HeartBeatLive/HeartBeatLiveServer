package com.munoon.heartbeatlive.server.config

import graphql.language.IntValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.time.Instant

@Configuration
class GraphqlScalarConfig {
    @Bean
    fun runtimeWiringConfigurer() = RuntimeWiringConfigurer {
        it.scalar(graphqlInstantScalar())
    }

    @Bean
    fun graphqlInstantScalar(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("EpochSecondsTime")
        .description("Time represented in epoch formats.")
        .coercing(object : Coercing<Any, Any> {
            override fun serialize(dataFetcherResult: Any): Long? {
                return dataFetcherResult.takeIf { dataFetcherResult is Instant }
                    ?.let { dataFetcherResult as Instant }
                    ?.epochSecond
            }

            override fun parseValue(input: Any): Instant {
                if (input !is Number) {
                    throw CoercingSerializeException("EpochSecondsTime scalar should be a number.")
                }

                try {
                    return Instant.ofEpochSecond(input.toLong())
                } catch (e: Exception) {
                    throw CoercingSerializeException("Can't parse EpochSecondsTime scalar", e)
                }
            }

            override fun parseLiteral(input: Any): Instant {
                if (input !is IntValue) {
                    throw CoercingSerializeException("EpochSecondsTime scalar should be a number.")
                }

                try {
                    return Instant.ofEpochSecond(input.value.toLong())
                } catch (e: Exception) {
                    throw CoercingSerializeException("Can't parse EpochSecondsTime scalar", e)
                }
            }
        })
        .build()
}