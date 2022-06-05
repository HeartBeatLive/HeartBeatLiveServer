package com.munoon.heartbeatlive.server.subscription.service

import com.munoon.heartbeatlive.server.ban.UserBanEvents
import com.munoon.heartbeatlive.server.ban.UserBanUtils.validateUserBanned
import com.munoon.heartbeatlive.server.ban.service.UserBanService
import com.munoon.heartbeatlive.server.common.PageResult
import com.munoon.heartbeatlive.server.config.properties.SubscriptionProperties
import com.munoon.heartbeatlive.server.sharing.HeartBeatSharingUtils.checkExpired
import com.munoon.heartbeatlive.server.sharing.service.HeartBeatSharingService
import com.munoon.heartbeatlive.server.subscription.SelfSubscriptionAttemptException
import com.munoon.heartbeatlive.server.subscription.Subscription
import com.munoon.heartbeatlive.server.subscription.SubscriptionEvent
import com.munoon.heartbeatlive.server.subscription.SubscriptionNotFoundByIdException
import com.munoon.heartbeatlive.server.subscription.SubscriptionUtils.validateUserSubscribersCount
import com.munoon.heartbeatlive.server.subscription.SubscriptionUtils.validateUserSubscriptionsCount
import com.munoon.heartbeatlive.server.subscription.account.service.AccountSubscriptionService
import com.munoon.heartbeatlive.server.subscription.repository.SubscriptionRepository
import com.munoon.heartbeatlive.server.user.UserEvents
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class SubscriptionService(
    private val repository: SubscriptionRepository,
    private val heartBeatSharingService: HeartBeatSharingService,
    private val accountSubscriptionService: AccountSubscriptionService,
    private val subscriptionProperties: SubscriptionProperties,
    private val userBanService: UserBanService,
    private val eventPublisher: ApplicationEventPublisher
) {
    suspend fun subscribeBySharingCode(code: String, userId: String): Subscription {
        val sharingCode = heartBeatSharingService.getSharingCodeByPublicCode(code)

        // validation stage
        if (sharingCode.userId == userId) {
            throw SelfSubscriptionAttemptException()
        }
        sharingCode.checkExpired()
        validateUserSubscribersCount(sharingCode.userId)
        validateUserSubscriptionsCount(userId)
        userBanService.validateUserBanned(userId, sharingCode.userId)

        // return existing subscription if it exists
        repository.findByUserIdAndSubscriberUserId(userId = sharingCode.userId, subscriberUserId = userId)
            ?.let { return it }

        return repository.save(Subscription(
            userId = sharingCode.userId,
            subscriberUserId = userId
        )).also { subscription ->
            eventPublisher.publishEvent(SubscriptionEvent.SubscriptionCreatedEvent(subscription))
        }
    }

    suspend fun unsubscribeFromUserById(id: String, validateUserId: String?) {
        val count = when (validateUserId) {
            null -> repository.deleteSubscriptionById(id)
            else -> repository.deleteSubscriptionByIdAndSubscriberUserId(id, validateUserId)
        }

        if (count <= 0) {
            throw SubscriptionNotFoundByIdException(id)
        }
    }

    suspend fun getSubscriptionById(id: String): Subscription {
        return repository.findById(id) ?: throw SubscriptionNotFoundByIdException(id)
    }

    suspend fun getSubscribers(userId: String, pageable: Pageable): PageResult<Subscription> {
        return PageResult(
            data = repository.findAllByUserId(userId, pageable).asFlow(),
            totalItemsCount = repository.countAllByUserId(userId)
        )
    }

    suspend fun getSubscriptions(userId: String, pageable: Pageable): PageResult<Subscription> {
        return PageResult(
            data = repository.findAllBySubscriberUserId(userId, pageable).asFlow(),
            totalItemsCount = repository.countAllBySubscriberUserId(userId)
        )
    }

    suspend fun checkUserHaveMaximumSubscribers(userId: String): Boolean {
        val subscribersCount = repository.countAllByUserId(userId)
        val userSubscriptionPlan = accountSubscriptionService.getAccountSubscriptionByUserId(userId).subscriptionPlan
        return subscribersCount >= subscriptionProperties[userSubscriptionPlan].maxSubscribersLimit
    }

    suspend fun checkUserHaveMaximumSubscriptions(userId: String): Boolean {
        val subscribersCount = repository.countAllBySubscriberUserId(userId)
        val userSubscriptionPlan = accountSubscriptionService.getAccountSubscriptionByUserId(userId).subscriptionPlan
        return subscribersCount >= subscriptionProperties[userSubscriptionPlan].maxSubscriptionsLimit
    }

    @Async
    @EventListener
    fun handleUserDeletedEvent(event: UserEvents.UserDeletedEvent) {
        runBlocking { repository.deleteAllByUserIdOrSubscriberUserId(event.userId) }
    }

    @Async
    @EventListener
    fun handleUserBannedEvent(event: UserBanEvents.UserBannedEvent) {
        runBlocking { repository.deleteAllBySubscriberUserIdAndUserId(event.userId, event.bannedByUserId) }
    }
}