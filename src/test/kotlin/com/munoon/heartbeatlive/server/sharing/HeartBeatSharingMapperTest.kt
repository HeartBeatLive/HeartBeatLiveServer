package com.munoon.heartbeatlive.server.sharing

import com.munoon.heartbeatlive.server.config.properties.UserSharingProperties
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.asGraphQL
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.asPublicGraphQL
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingMapper.create
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.model.GraphqlPublicSharingCode
import com.munoon.heartbeatlive.server.sharing.model.GraphqlSharingCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.Duration
import java.time.Instant

internal class HeartBeatSharingMapperTest {
    @Test
    fun create() {
        val expiresAt = Instant.now().plus(Duration.ofDays(10))
        val expected = HeartBeatSharing(
            id = null,
            publicCode = "random",
            userId = "user1",
            expiredAt = expiresAt
        )

        val actual = GraphqlCreateSharingCodeInput(expiredAt = expiresAt).create("user1")

        assertThat(actual)
            .usingRecursiveComparison()
            .ignoringFields("created", "publicCode")
            .isEqualTo(expected)

        assertThat(actual.publicCode.length).isEqualTo(6)
    }

    @Test
    fun asGraphQL() {
        val created = Instant.now()
        val expiresAt = created.plus(Duration.ofDays(10))

        val expected = GraphqlSharingCode(
            id = "sharingCode1",
            publicCode = "ABC123",
            sharingUrl = "https://heartbeatsharing.com/sharing/ABC123",
            created = created,
            expiredAt = expiresAt,
            locked = true,
            userId = "user1"
        )

        val sharingCode = HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = expiresAt,
            locked = true,
            created = created
        )

        val properties = UserSharingProperties().apply {
            sharingUrlTemplate = "https://heartbeatsharing.com/sharing/%s"
        }

        val actual = sharingCode.asGraphQL(properties)
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun asPublicGraphQL() {
        val created = Instant.now()
        val expiresAt = created.plus(Duration.ofDays(10))

        val expected = GraphqlPublicSharingCode(
            publicCode = "ABC123",
            sharingUrl = "https://heartbeatsharing.com/sharing/ABC123",
            created = created,
            expiredAt = expiresAt,
            userId = "user1"
        )

        val sharingCode = HeartBeatSharing(
            id = "sharingCode1",
            publicCode = "ABC123",
            userId = "user1",
            expiredAt = expiresAt,
            created = created
        )

        val properties = UserSharingProperties().apply {
            sharingUrlTemplate = "https://heartbeatsharing.com/sharing/%s"
        }

        val actual = sharingCode.asPublicGraphQL(properties)
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}