package com.munoon.heartbeatlive.server.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PageResult<T>(
    val data: Flow<T>,
    val totalItemsCount: Int
) {
    fun <M> map(mapper: (T) -> M): PageResult<M> = PageResult(data.map(mapper), totalItemsCount)
}