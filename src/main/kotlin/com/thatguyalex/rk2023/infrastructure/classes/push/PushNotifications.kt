package com.thatguyalex.rk2023.infrastructure.classes.push

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("push_subscriptions")
data class PushSubscription(
    @Id
    val id: Long? = null,
    val endpoint: String,
    val p256dh: String,
    val auth: String
)

@Table("push_subscriptions_topics")
data class PushSubscriptionTopic(
    @Id
    val id: Long? = null,
    val pushSubscriptionId: Long,
    val electionType: ElectionType,
    val topicType: PushTopicType,
    val topicCode: String
)

enum class PushTopicType(val smallName: String) {
    REGION("R"),
    CANDIDATE("C"),
}

data class ElectionPushMessage(
    val electionType: ElectionType,
    val topicType: PushTopicType,
    val topicCode: String,
    val title: String,
    val body: String,
) {
    val pushMessage = PushMessage(
        title = title,
        body = body,
        topic = "${electionType.smallName}-${topicType.smallName}-$topicCode",
    )
}

data class PushMessage(
    val title: String,
    val body: String,
    val url: String = "",
    val topic: String? = null,
)

enum class PushSendResult {
    SUCCESS,
    EXPIRED,
    FAILED,
    SERVICE_DISABLED,
}