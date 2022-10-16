package com.munoon.heartbeatlive.server.apple

import com.munoon.heartbeatlive.server.AbstractTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    properties = [
        "apple.app-ids[0]=XXX.com.munoon.heartbeatlive",
        "apple.app-ids[1]=YYY.com.munoon.heartbeatlive"
    ]
)
@AutoConfigureWebTestClient
internal class AppleWellKnownApiControllerTest : AbstractTest() {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun getAppleAppSiteAssociation() {
        webTestClient.get()
            .uri("/.well-known/apple-app-site-association")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.webcredentials").isArray()
            .jsonPath("$.webcredentials[0]").isEqualTo("XXX.com.munoon.heartbeatlive")
            .jsonPath("$.webcredentials[1]").isEqualTo("YYY.com.munoon.heartbeatlive")
    }
}