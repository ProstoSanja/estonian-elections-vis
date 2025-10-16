package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.infrastructure.classes.push.PushSubscription
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PushSubscriptionRepo : CrudRepository<PushSubscription, Long> {
    fun findByEndpoint(endpoint: String): PushSubscription?
    
    @Query("SELECT * FROM push_subscriptions WHERE id IN :ids")
    fun findAllByIds(ids: List<Long>): List<PushSubscription>
    
    @Transactional
    fun deleteByEndpoint(endpoint: String): Long
}