package com.munoon.heartbeatlive.server.sharing.model

import java.time.Instant
import javax.validation.constraints.Future

data class GraphqlCreateSharingCodeInput(
    @field:Future
    val expiredAt: Instant?
)
