package com.munoon.heartbeatlive.server.ban

import com.munoon.heartbeatlive.server.ban.UserBanMapper.asGraphql
import com.munoon.heartbeatlive.server.ban.model.GraphqlBanInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

internal class UserBanMapperTest {
    @Test
    fun asGraphql() {
        val created = Instant.now()
        val expected = GraphqlBanInfo(
            id = "userBan1",
            banTime = created,
            userId = "user1",
            bannedUserId = "user2"
        )

        val userBan = UserBan(
            id = "userBan1",
            userId = "user1",
            bannedUserId = "user2",
            created = created
        )
        val actual = userBan.asGraphql()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}