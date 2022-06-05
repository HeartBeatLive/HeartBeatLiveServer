package com.munoon.heartbeatlive.server.ban.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.ban.UserBan
import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.ban.UserBanNotFoundByIdException
import com.munoon.heartbeatlive.server.ban.repository.UserBanRepository
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.user.UserEvents
import com.munoon.heartbeatlive.server.user.model.GraphqlFirebaseCreateUserInput
import com.munoon.heartbeatlive.server.user.service.UserService
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents

@SpringBootTest
@RecordApplicationEvents
internal class UserBanServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: UserBanService

    @Autowired
    private lateinit var repository: UserBanRepository

    @Autowired
    private lateinit var events: ApplicationEvents

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Autowired
    private lateinit var userService: UserService

    @Test
    fun `checkUserBanned - true`() {
        runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }

        val result = runBlocking { service.checkUserBanned(userId = "user2", bannedByUserId = "user1") }
        assertThat(result).isTrue
    }

    @Test
    fun `checkUserBanned - false`() {
        val result = runBlocking { service.checkUserBanned(userId = "user2", bannedByUserId = "user1") }
        assertThat(result).isFalse
    }

    @Test
    fun banUser() {
        runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(
            id = "user1", email = null, emailVerified = false)) }
        val actual = runBlocking { service.banUser(userId = "user1", userIdToBan = "user2") }
        val expected = UserBan(
            id = actual.id!!,
            userId = "user1",
            bannedUserId = "user2",
            created = actual.created
        )

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected))
        }
        assertThat(events.stream(UserBanEvents::class.java).toList())
            .usingRecursiveComparison()
            .isEqualTo(listOf(UserBanEvents.UserBannedEvent(userId = "user2", bannedByUserId = "user1")))
    }

    @Test
    fun `banUser - already banned`() {
        runBlocking { userService.createUser(GraphqlFirebaseCreateUserInput(
            id = "user1", email = null, emailVerified = false)) }
        val ban = runBlocking { service.banUser(userId = "user1", userIdToBan = "user2") }
        events.clear()

        val actual = runBlocking { service.banUser(userId = "user1", userIdToBan = "user2") }

        assertThat(actual).usingRecursiveComparison().ignoringFields("created").isEqualTo(ban)
        runBlocking { assertThat(repository.count()).isOne }
        assertThat(events.stream(UserBanEvents::class.java).count()).isZero
    }

    @Test
    fun `unbanUser without validation`() {
        val ban = runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }
        runBlocking { service.unbanUser(ban.id!!, validateUserId = null) }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `unbanUser with validation`() {
        val ban = runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }
        runBlocking { service.unbanUser(ban.id!!, validateUserId = "user1") }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `unbanUser without validation - not found`() {
        assertThatThrownBy { runBlocking { service.unbanUser("abc", validateUserId = null) } }
            .isEqualTo(UserBanNotFoundByIdException("abc"))
    }

    @Test
    fun `unbanUser with validation - not found`() {
        assertThatThrownBy { runBlocking { service.unbanUser("abc", validateUserId = "user1") } }
            .isEqualTo(UserBanNotFoundByIdException("abc"))
    }

    @Test
    fun `unbanUser with validation - invalid user`() {
        val ban = runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }
        assertThatThrownBy { runBlocking { service.unbanUser(ban.id!!, validateUserId = "user2") } }
            .isEqualTo(UserBanNotFoundByIdException(ban.id!!))
    }

    @Test
    fun getBannedUsers() {
        val ban1 = runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }
        val ban2 = runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user3")) }
        runBlocking { repository.save(UserBan(userId = "user2", bannedUserId = "user1")) }
        runBlocking { repository.save(UserBan(userId = "user2", bannedUserId = "user3")) }

        val expected = PageResult(
            data = flowOf(ban1, ban2),
            totalItemsCount = 2
        )

        val pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"))
        val actual = runBlocking { service.getBannedUsers("user1", pageRequest) }
        assertThat(actual).usingRecursiveComparison().ignoringFields("data").isEqualTo(expected)
        runBlocking {
            assertThat(actual.data.toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(ban1, ban2))
        }
    }

    @Test
    fun handleUserDeletedEvent() {
        runBlocking { repository.save(UserBan(userId = "user1", bannedUserId = "user2")) }
        runBlocking { repository.save(UserBan(userId = "user2", bannedUserId = "user1")) }
        val expected = runBlocking { repository.save(UserBan(userId = "user2", bannedUserId = "user3")) }
        runBlocking { assertThat(repository.count()).isEqualTo(3) }

        eventPublisher.publishEvent(UserEvents.UserDeletedEvent("user1", updateFirebaseState = false))

        runBlocking { assertThat(repository.count()).isOne }
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected))
        }
    }
}