package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.PushSubscriptionTopic
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface PushSubscriptionTopicRepo : CrudRepository<PushSubscriptionTopic, Long> {
    
    fun findByPushSubscriptionId(subscriptionId: Long): List<PushSubscriptionTopic>
    
    fun findByElectionType(electionType: ElectionType): List<PushSubscriptionTopic>
    
    fun findByElectionTypeAndTopicTypeAndTopicCode(
        electionType: ElectionType,
        topicType: String,
        topicCode: String
    ): List<PushSubscriptionTopic>
    
    @Transactional
    fun deleteByPushSubscriptionId(subscriptionId: Long): Long
    
    @Query("""
        SELECT DISTINCT pst.push_subscription_id 
        FROM push_subscriptions_topics pst 
        WHERE pst.election_type = :electionType
        AND pst.topic_type = :topicType 
        AND pst.topic_code = :topicCode
    """)
    fun findSubscriptionIdsByTopic(
        electionType: ElectionType,
        topicType: String,
        topicCode: String
    ): List<Long>
    
    @Query("""
        SELECT DISTINCT pst.push_subscription_id 
        FROM push_subscriptions_topics pst 
        WHERE pst.election_type = :electionType
    """)
    fun findSubscriptionIdsByElection(electionType: ElectionType): List<Long>
}

