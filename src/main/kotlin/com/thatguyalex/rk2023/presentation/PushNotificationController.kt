package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.PushNotificationApplication
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.PushNotificationsSender
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionTopicRepo
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.push.PushMessage
import com.thatguyalex.rk2023.presentation.classes.push.PushSubscriptionDto
import com.thatguyalex.rk2023.presentation.classes.push.UnsubscribeDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/push")
class PushNotificationController(
    private val subscriptionRepo: PushSubscriptionRepo,
    private val topicRepo: PushSubscriptionTopicRepo,
    private val pushNotificationApplication: PushNotificationApplication,
    @Value("\${push.admin.key}") private val adminPushKey: String,
) {

    @PostMapping("/subscribe")
    fun subscribe(@RequestBody subscriptionDto: PushSubscriptionDto) {
        // Check if subscription already exists by endpoint
        val existing = subscriptionRepo.findByEndpoint(subscriptionDto.endpoint)
        
        val subscription = if (existing != null) {
            // Update existing subscription
            val updated = subscriptionDto.toEntity().copy(id = existing.id)
            subscriptionRepo.save(updated)
        } else {
            // Create new subscription
            subscriptionRepo.save(subscriptionDto.toEntity())
        }

        if (subscriptionDto.dashboardEntries.isNotEmpty()) {
            // Delete old topics and create new ones
            subscription.id!!.let { subscriptionId ->
                topicRepo.deleteByPushSubscriptionId(subscriptionId)
                val topics = subscriptionDto.toTopics(subscriptionId)
                topicRepo.saveAll(topics)
            }
        }
    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(@RequestBody unsubscribeDto: UnsubscribeDto) {
        subscriptionRepo.deleteByEndpoint(unsubscribeDto.endpoint)
    }

    @GetMapping("/announce")
    fun sendTestNotification(@RequestParam electionType: ElectionType, @RequestParam password: String, @RequestParam message: String?): Int {
        if (password != adminPushKey) {
            throw IllegalArgumentException("Wrong password")
        }
        return pushNotificationApplication.sendMessageToAllElectionSubscribers(electionType, message)
    }
}


