package com.munoon.heartbeatlive.server.common

import com.munoon.heartbeatlive.server.common.CommonUtils.asGraphqlPage
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommonUtilsTest {
    @Test
    fun asGraphqlPage() {
        val graphqlPageResult = GraphqlPageResult(
            pageInfo = PageInfo(totalItems = 10, totalPages = 4, hasNext = true),
            content = listOf(4, 5, 6)
        )

        // all items: 1...10
        // represent page = 1, size = 3
        val pageResult = PageResult(data = listOf(4, 5, 6).asFlow(), 10)

        val graphqlPage = runBlocking { pageResult.asGraphqlPage(1, 3) }
        assertThat(graphqlPage).usingRecursiveComparison().isEqualTo(graphqlPageResult)
    }
}