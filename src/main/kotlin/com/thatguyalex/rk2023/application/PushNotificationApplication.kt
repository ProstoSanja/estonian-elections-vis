package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionSender
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionTopicRepo
import com.thatguyalex.rk2023.infrastructure.classes.PushMessage
import org.springframework.stereotype.Service

@Service
class PushNotificationApplication(
    private val subscriptionRepo: PushSubscriptionRepo,
    private val topicRepo: PushSubscriptionTopicRepo,
    private val sender: PushSubscriptionSender
) {
    
    fun sendNotificationToElection(electionType: ElectionType, message: PushMessage): Int {
        val subscriptionIds = topicRepo.findSubscriptionIdsByElection(electionType)
        val subscriptions = subscriptionRepo.findAllByIds(subscriptionIds)
        return sender.sendNotificationTo(subscriptions, message)
    }
    
    fun sendNotificationToTopic(
        electionType: ElectionType, 
        topicType: String, 
        topicCode: String, 
        message: PushMessage
    ): Int {
        val subscriptionIds = topicRepo.findSubscriptionIdsByTopic(electionType, topicType, topicCode)
        val subscriptions = subscriptionRepo.findAllByIds(subscriptionIds)
        return sender.sendNotificationTo(subscriptions, message)
    }
}
