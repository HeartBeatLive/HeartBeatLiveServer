package com.munoon.heartbeatlive.server.common

import graphql.GraphQLContext
import kotlinx.coroutines.flow.toList
import kotlin.math.ceil

object CommonUtils {
    suspend fun <T> PageResult<T>.asGraphqlPage(pageNumber: Int, pageSize: Int): GraphqlPageResult<T> {
        val totalPages = if (pageSize == 0) 1 else ceil(totalItemsCount.toDouble() / pageSize.toDouble()).toInt()
        val pageInfo = PageInfo(
            totalPages = totalPages,
            totalItems = totalItemsCount,
            hasNext = pageNumber + 1 < totalPages
        )
        return GraphqlPageResult(pageInfo, data.toList(arrayListOf()))
    }

    fun graphqlContextOf(vararg pairs: Pair<Any, Any>): GraphQLContext = GraphQLContext.of(pairs.toMap())
}