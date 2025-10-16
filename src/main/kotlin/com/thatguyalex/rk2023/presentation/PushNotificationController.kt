package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.PushNotificationApplication
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionSender
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionTopicRepo
import com.thatguyalex.rk2023.infrastructure.classes.PushMessage
import com.thatguyalex.rk2023.infrastructure.classes.PushSubscriptionDto
import com.thatguyalex.rk2023.infrastructure.classes.UnsubscribeDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/push")
class PushNotificationController(
    private val subscriptionRepo: PushSubscriptionRepo,
    private val topicRepo: PushSubscriptionTopicRepo,
    private val sender: PushSubscriptionSender
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

    @PostMapping("/test")
    fun sendTestNotification(): Int {
        return sender.sendNotificationTo(subscriptionRepo.findAll().toList(), PushMessage(
            title = "Test Title",
            body = "Test message"
        ))
    }
}


