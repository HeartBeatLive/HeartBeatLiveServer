package com.munoon.heartbeatlive.server.apple

import com.munoon.heartbeatlive.server.config.properties.AppleProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/.well-known")
class AppleWellKnownApiController(
    private val properties: AppleProperties
) {
    private companion object {
        const val WEB_CREDENTIALS_KEY = "webcredentials"
    }

    @GetMapping("/apple-app-site-association")
    fun getAppleAppSiteAssociation(): Map<String, Any> = mapOf(
        WEB_CREDENTIALS_KEY to properties.appIds
    )
}