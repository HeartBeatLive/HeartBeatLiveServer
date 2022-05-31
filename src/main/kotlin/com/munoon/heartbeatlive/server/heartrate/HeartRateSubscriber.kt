package com.munoon.heartbeatlive.server.heartrate

import com.munoon.heartbeatlive.server.config.ServerInstanceRunningId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("heartRateSubscriber")
data class HeartRateSubscriber(
    @Id
    val id: String? = null,

    val userId: String,

    val subscriptionTime: Instant = Instant.now(),

    val serverId: String = ServerInstanceRunningId.id
)