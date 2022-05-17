package com.munoon.heartbeatlive.server.common

data class GraphqlPageResult<T>(
    val pageInfo: PageInfo,
    val content: List<T>
)

data class PageInfo(
    val totalPages: Int,
    val totalItems: Int,
    val hasNext: Boolean
)