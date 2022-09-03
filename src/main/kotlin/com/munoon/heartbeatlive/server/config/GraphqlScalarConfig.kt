package com.munoon.heartbeatlive.server.config

import graphql.TypeResolutionEnvironment
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.scalar.GraphqlStringCoercing
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLCodeRegistry
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLScalarType
import graphql.schema.TypeResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

@Configuration
class GraphqlScalarConfig {
    @Bean
    fun runtimeWiringConfigurer(typeResolvers: List<GraphqlTypeResolver>) = RuntimeWiringConfigurer {
        it.scalar(graphqlInstantScalar())
        it.scalar(graphqlCurrencyScalar())
        it.scalar(graphqlDecimalScalar())
        it.scalar(graphqlDurationScalar())

        val codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry()
        for (typeResolver in typeResolvers) {
            codeRegistryBuilder.typeResolver(typeResolver.typeName, typeResolver)
        }
        it.codeRegistry(codeRegistryBuilder)
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

    @Bean
    fun graphqlCurrencyScalar(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Currency")
        .description("Alphabetic currency name in ISO 4217 format.")
        .coercing(GraphqlStringCoercing())
        .build()

    @Bean
    fun graphqlDecimalScalar(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Decimal")
        .description("Decimal number, which supports arbitrary precision and is serialized as a string.")
        .coercing(object : Coercing<Any, Any> {
            override fun serialize(dataFetcherResult: Any) = (dataFetcherResult as? BigDecimal)?.toString()

            override fun parseValue(input: Any): Any {
                if (input is Number) {
                    return BigDecimal(input.toDouble())
                }
                if (input is String) {
                    return BigDecimal(input)
                }
                throw CoercingSerializeException("Decimal scalar must be either String or Number.")
            }

            override fun parseLiteral(input: Any): Any {
                if (input is IntValue) {
                    return BigDecimal(input.value)
                }
                if (input is FloatValue) {
                    return BigDecimal(input.value.toDouble())
                }
                if (input is StringValue) {
                    return BigDecimal(input.value)
                }
                throw CoercingSerializeException("Decimal scalar must be either Int, Float or String.")
            }
        })
        .build()

    @Bean
    fun graphqlDurationScalar(): GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Duration")
        .description("Duration, represented in seconds.")
        .coercing(object : Coercing<Any, Any> {
            override fun serialize(dataFetcherResult: Any) = (dataFetcherResult as? Duration)?.toSeconds()

            override fun parseValue(input: Any): Any {
                if (input is Number) {
                    return Duration.ofSeconds(input.toLong())
                }
                throw CoercingSerializeException("Decimal scalar must be either String or Number.")
            }

            override fun parseLiteral(input: Any): Any {
                if (input is IntValue) {
                    return Duration.ofSeconds(input.value.toLong())
                }
                throw CoercingSerializeException("Decimal scalar must be either Int, Float or String.")
            }
        })
        .build()
}

interface GraphqlTypeResolver : TypeResolver {
    val typeName: String
}

abstract class AbstractGraphqlTypeNameResolver<T>(override val typeName: String) : GraphqlTypeResolver {
    override fun getType(env: TypeResolutionEnvironment): GraphQLObjectType {
        val obj = env.getObject<T>()
        val typeName = getTypeName(obj)
        return env.schema.getObjectType(typeName)
    }

    abstract fun getTypeName(obj: T): String
}