package com.thatguyalex.rk2023.infrastructure.classes

data class PushSubscription(
    val endpoint: String,
    val keys: PushSubscriptionKeys
)

data class PushSubscriptionKeys(
    val p256dh: String,
    val auth: String
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