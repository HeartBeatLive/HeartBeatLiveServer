package com.munoon.heartbeatlive.server.versioning

import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ApplicationMinVersionController {
    @QueryMapping
    fun applicationMinVersion() = ApplicationMinVersion(
        versionNumber = "1.0",
        required = false,
        description = "First release"
    )
}

data class ApplicationMinVersion(
    val versionNumber: String,
    val required: Boolean,
    val description: String
)