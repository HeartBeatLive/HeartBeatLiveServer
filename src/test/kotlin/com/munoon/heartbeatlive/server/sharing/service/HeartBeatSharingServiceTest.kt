package com.munoon.heartbeatlive.server.sharing.service

import com.munoon.heartbeatlive.server.AbstractTest
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharing
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingLimitExceededException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByIdException
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingNotFoundByPublicCodeException
import com.munoon.heartbeatlive.server.sharing.model.GraphqlCreateSharingCodeInput
import com.munoon.heartbeatlive.server.sharing.repository.HeartBeatSharingRepository
import com.munoon.heartbeatlive.server.subscription.account.UserSubscriptionPlan
import com.munoon.heartbeatlive.server.user.UserEvents
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
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime

@SpringBootTest
internal class HeartBeatSharingServiceTest : AbstractTest() {
    @Autowired
    private lateinit var service: HeartBeatSharingService

    @Autowired
    private lateinit var repository: HeartBeatSharingRepository

    @Autowired
    private lateinit var eventPublisher: ApplicationEventPublisher

    @Test
    fun createSharing() {
        val userId = "user1"
        val expiresAt = Instant.now().plus(Duration.ofDays(5))

        val expectedSharingCode = HeartBeatSharing(
            id = null,
            publicCode = "random",
            userId = userId,
            expiredAt = expiresAt
        )

        val input = GraphqlCreateSharingCodeInput(expiredAt = expiresAt)
        val sharingCode = runBlocking {
            service.createSharing(input, userId, UserSubscriptionPlan.FREE)
        }

        assertThat(sharingCode)
            .usingRecursiveComparison().ignoringFields("id", "publicCode", "created")
            .isEqualTo(expectedSharingCode)
        assertThat(sharingCode.publicCode).isNotBlank
        assertThat(sharingCode.id).isNotBlank

        val expectedSharingCodes = listOf(HeartBeatSharing(
            id = sharingCode.id,
            publicCode = sharingCode.publicCode,
            userId = userId,
            created = sharingCode.created,
            expiredAt = expiresAt
        ))

        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison().ignoringFields("created", "expiredAt")
                .isEqualTo(expectedSharingCodes)
        }
    }

    @Test
    fun `createSharing - limit exceeded`() {
        val userId = "userId"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        runBlocking {
            service.createSharing(input, userId, UserSubscriptionPlan.FREE)
            service.createSharing(input, userId, UserSubscriptionPlan.FREE)
        }

        assertThatThrownBy { runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) } }
            .isEqualTo(HeartBeatSharingLimitExceededException(limit = 2))
    }

    @Test
    fun getSharingCodeById() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }

        val actual = runBlocking { service.getSharingCodeById(sharingCode.id!!) }

        val expected = HeartBeatSharing(
            id = actual.id,
            publicCode = actual.publicCode,
            userId = userId,
            expiredAt = null
        )
        assertThat(actual).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        assertThat(actual.id).isNotBlank
        assertThat(actual.publicCode).isNotBlank
    }

    @Test
    fun `getSharingCodeById - not found`() {
        assertThatThrownBy { runBlocking { service.getSharingCodeById("abc") } }
            .isEqualTo(HeartBeatSharingNotFoundByIdException(id = "abc"))
    }

    @Test
    fun getSharingCodeByPublicCode() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }

        val actual = runBlocking { service.getSharingCodeByPublicCode(sharingCode.publicCode) }

        val expected = HeartBeatSharing(
            id = actual.id,
            publicCode = actual.publicCode,
            userId = userId,
            expiredAt = null
        )
        assertThat(actual).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        assertThat(actual.id).isNotBlank
        assertThat(actual.publicCode).isNotBlank
    }

    @Test
    fun `getSharingCodeByPublicCode - not found`() {
        assertThatThrownBy { runBlocking { service.getSharingCodeByPublicCode("abc") } }
            .isEqualTo(HeartBeatSharingNotFoundByPublicCodeException(publicCode = "abc"))
    }

    @Test
    fun updateSharingCodeExpireTime() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val newExpireAt = OffsetDateTime.now().plusDays(10).withNano(0).withSecond(0).toInstant()
        val sharingCode = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }

        val expected = HeartBeatSharing(
            id = sharingCode.id,
            publicCode = sharingCode.publicCode,
            userId = userId,
            expiredAt = newExpireAt,
            created = sharingCode.created
        )

        val updatedShareCode = runBlocking {
            service.updateSharingCodeExpireTime(sharingCode.id!!, newExpireAt, validateUserId = null)
        }

        assertThat(updatedShareCode).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected))
        }
    }

    @Test
    fun `updateSharingCodeExpireTime - not found`() {
        assertThatThrownBy { runBlocking {
            service.updateSharingCodeExpireTime("abc", expiredAt = null, validateUserId = null)
        } }.isEqualTo(HeartBeatSharingNotFoundByIdException(id = "abc"))
    }

    @Test
    fun `updateSharingCodeExpireTime with user validation`() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val newExpireAt = OffsetDateTime.now().plusDays(10).withNano(0).withSecond(0).toInstant()
        val sharingCode = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }

        val expected = HeartBeatSharing(
            id = sharingCode.id,
            publicCode = sharingCode.publicCode,
            userId = userId,
            expiredAt = newExpireAt,
            created = sharingCode.created
        )

        val updatedShareCode = runBlocking {
            service.updateSharingCodeExpireTime(sharingCode.id!!, newExpireAt, validateUserId = userId)
        }

        assertThat(updatedShareCode).usingRecursiveComparison().ignoringFields("created").isEqualTo(expected)
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(expected))
        }
    }

    @Test
    fun `updateSharingCodeExpireTime with user validation - invalid user`() {
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.FREE) }

        assertThatThrownBy { runBlocking {
            service.updateSharingCodeExpireTime(sharingCode.id!!, Instant.now(), "user2")
        } }.isEqualTo(HeartBeatSharingNotFoundByIdException(id = sharingCode.id!!))
    }

    @Test
    fun deleteSharingCodeById() {
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.FREE) }

        runBlocking { assertThat(repository.count()).isOne }
        runBlocking { service.deleteSharingCodeById(sharingCode.id!!, validateUserId = null) }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `deleteSharingCodeById - not found`() {
        assertThatThrownBy {
            runBlocking { service.deleteSharingCodeById(id = "abc", validateUserId = null) }
        }.isEqualTo(HeartBeatSharingNotFoundByIdException(id = "abc"))
    }

    @Test
    fun `deleteSharingCodeById with user validation`() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }

        runBlocking { assertThat(repository.count()).isOne }
        runBlocking { service.deleteSharingCodeById(sharingCode.id!!, validateUserId = userId) }
        runBlocking { assertThat(repository.count()).isZero }
    }

    @Test
    fun `deleteSharingCodeById with user validation - not found`() {
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        val sharingCode = runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.FREE) }

        assertThatThrownBy { runBlocking {
            service.deleteSharingCodeById(sharingCode.id!!, "user2")
        } }.isEqualTo(HeartBeatSharingNotFoundByIdException(id = sharingCode.id!!))
    }

    @Test
    fun getSharingCodesByUserId() {
        val userId = "user1"
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)

        val sharingCode1 = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }
        val sharingCode2 = runBlocking { service.createSharing(input, userId, UserSubscriptionPlan.FREE) }
        runBlocking { service.createSharing(input, "user2", UserSubscriptionPlan.FREE) } // ignored, because belong to another user

        runBlocking {
            val pageRequest = PageRequest.of(0, 10, Sort.Direction.DESC, "created")
            val pageResult = service.getSharingCodesByUserId(pageRequest, userId)
            assertThat(pageResult.totalItemsCount).isEqualTo(2)
            assertThat(pageResult.data.toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(sharingCode2, sharingCode1))
        }
    }

    @Test
    fun handleUserDeletedEvent() {
        val input = GraphqlCreateSharingCodeInput(expiredAt = null)
        runBlocking { service.createSharing(input, "user1", UserSubscriptionPlan.FREE) }
        val user2SharingCode = runBlocking { service.createSharing(input, "user2", UserSubscriptionPlan.FREE) }
        runBlocking { assertThat(repository.count()).isEqualTo(2) }

        eventPublisher.publishEvent(UserEvents.UserDeletedEvent(userId = "user1", updateFirebaseState = false))

        runBlocking { assertThat(repository.count()).isEqualTo(1) }
        runBlocking {
            assertThat(repository.findAll().toList(arrayListOf()))
                .usingRecursiveComparison()
                .ignoringFields("created")
                .isEqualTo(listOf(user2SharingCode))
        }
    }
}