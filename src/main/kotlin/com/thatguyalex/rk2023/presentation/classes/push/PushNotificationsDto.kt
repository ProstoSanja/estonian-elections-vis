package com.thatguyalex.rk2023.presentation.classes.push

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.push.PushSubscription
import com.thatguyalex.rk2023.infrastructure.classes.push.PushSubscriptionTopic

data class PushSubscriptionKeysDto(
    val p256dh: String,
    val auth: String
)

// Dashboard entry types matching frontend
data class DashboardEntryDto(
    val type: String, // 'region' or 'candidate'
    val code: String
)

data class PushSubscriptionDto(
    val endpoint: String,
    val keys: PushSubscriptionKeysDto,
    val electionType: ElectionType,
    val dashboardEntries: List<DashboardEntryDto> = emptyList(),
) {
    fun toEntity(): PushSubscription {
        return PushSubscription(
            id = null,
            endpoint = endpoint,
            p256dh = keys.p256dh,
            auth = keys.auth
        )
    }
    
    fun toTopics(subscriptionId: Long): List<PushSubscriptionTopic> {
        return dashboardEntries.map { entry ->
            PushSubscriptionTopic(
                id = null,
                pushSubscriptionId = subscriptionId,
                electionType = electionType,
                topicType = entry.type,
                topicCode = entry.code
            )
        }
    }
}

data class UnsubscribeDto(
    val endpoint: String
)
