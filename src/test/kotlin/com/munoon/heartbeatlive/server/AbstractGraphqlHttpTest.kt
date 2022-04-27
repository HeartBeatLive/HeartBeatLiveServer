package com.munoon.heartbeatlive.server

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.reactive.server.WebTestClient

abstract class AbstractGraphqlHttpTest : AbstractTest() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    protected lateinit var graphqlTester: HttpGraphQlTester

    @BeforeEach
    fun buildGraphQlTester() {
        val webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
            .apply(springSecurity())
            .configureClient()
            .baseUrl("/graphql")
            .build()

        graphqlTester = HttpGraphQlTester.create(webTestClient)
    }
}