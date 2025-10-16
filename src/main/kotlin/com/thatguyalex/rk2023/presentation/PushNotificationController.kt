package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.PushNotificationService
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.classes.PushMessage
import com.thatguyalex.rk2023.infrastructure.classes.PushSubscription
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/push")
class PushNotificationController(
    private val subscriptionRepo: PushSubscriptionRepo,
    private val pushNotificationService: PushNotificationService
) {

    @PostMapping("/subscribe")
    fun subscribe(@RequestBody subscription: PushSubscription) {
        subscriptionRepo.save(subscription)
    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(@RequestBody subscription: PushSubscription) {
        subscriptionRepo.remove(subscription.endpoint)
    }

    @PostMapping("/test")
    fun sendTestNotification(): Int {
        return pushNotificationService.sendNotificationTo(subscriptionRepo.getAll(), PushMessage(
            title = "Test Title",
            body = "Test message"
        ))
    }
}


