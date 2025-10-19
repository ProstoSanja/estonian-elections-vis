package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.PushNotificationsSender
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionRepo
import com.thatguyalex.rk2023.infrastructure.PushSubscriptionTopicRepo
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.push.ElectionPushMessage
import com.thatguyalex.rk2023.infrastructure.classes.push.PushMessage
import com.thatguyalex.rk2023.infrastructure.classes.push.PushSubscription
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PushNotificationApplication(
    private val subscriptionRepo: PushSubscriptionRepo,
    private val topicRepo: PushSubscriptionTopicRepo,
    private val sender: PushNotificationsSender
) {
    private val logger = LoggerFactory.getLogger(PushNotificationApplication::class.java)

    fun sendMessageToAllElectionSubscribers(electionType: ElectionType): Int {
        return topicRepo.findSubscriptionIdsByElection(electionType)
            .let { subscriptionRepo.findAllByIds(it) }
            .let { sender.sendNotificationTo(it, PushMessage(
                title = "$electionType Valimised",
                body = "Hääletamine on lõppenud",
            )) }

    }

    fun sendElectionMessages(
        messages: List<ElectionPushMessage>
    ): Int {
        return messages.associateWith { topicRepo.findSubscriptionIdsByTopic(it.electionType, it.topicType, it.topicCode) }
            .filterValues { it.isNotEmpty() }
            .map { (message, subscriptionIds) ->
                val subscriptions = subscriptionRepo.findAllByIds(subscriptionIds)
                logger.info("Sending notification for ${message.topicType} ${message.topicCode} to ${subscriptions.size} subscriptions")
                sender.sendNotificationTo(subscriptions, message.pushMessage)
//                    .also { Thread.sleep(1000) }
            }
            .sum()
    }
}