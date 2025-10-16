package com.thatguyalex.rk2023.infrastructure.classes

import com.thatguyalex.rk2023.application.classes.ElectionType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("push_subscriptions")
data class PushSubscription(
    @Id
    val id: Long? = null,
    val endpoint: String,
    val p256dh: String,
    val auth: String
) {
    val keys: PushSubscriptionKeys
        get() = PushSubscriptionKeys(p256dh, auth)
}

@Table("push_subscriptions_topics")
data class PushSubscriptionTopic(
    @Id
    val id: Long? = null,
    val pushSubscriptionId: Long,
    val electionType: ElectionType,
    val topicType: String, // 'region' or 'candidate'
    val topicCode: String
)

data class PushSubscriptionKeys(
    val p256dh: String,
    val auth: String
)

// Dashboard entry types matching frontend
data class DashboardEntry(
    val type: String, // 'region' or 'candidate'
    val code: String
)

data class PushSubscriptionDto(
    val endpoint: String,
    val keys: PushSubscriptionKeys,
    val electionType: ElectionType,
    val dashboardEntries: List<DashboardEntry> = emptyList(),
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

data class PushMessage(
    val title: String,
    val body: String,
    val url: String = "",
)

enum class PushSendResult {
    SUCCESS,
    EXPIRED,
    FAILED,
    SERVICE_DISABLED,
}