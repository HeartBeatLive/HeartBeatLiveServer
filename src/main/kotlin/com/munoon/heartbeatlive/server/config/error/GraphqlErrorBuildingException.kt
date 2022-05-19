package com.munoon.heartbeatlive.server.config.error

import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment

interface GraphqlErrorBuildingException {
    fun build(env: DataFetchingEnvironment): GraphQLError
}