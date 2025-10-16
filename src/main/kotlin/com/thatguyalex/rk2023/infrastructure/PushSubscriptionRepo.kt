package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.infrastructure.classes.PushSubscription
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

@Repository
class PushSubscriptionRepo {
    // In-memory storage for PoC. In production, use a database.
    private val subscriptions = ConcurrentHashMap<String, PushSubscription>()

    fun save(subscription: PushSubscription) {
        subscriptions[subscription.endpoint] = subscription
    }

    fun remove(endpoint: String) {
        subscriptions.remove(endpoint)
    }

    fun getAll(): List<PushSubscription> {
        return subscriptions.values.toList()
    }

    fun count(): Int {
        return subscriptions.size
    }
}
