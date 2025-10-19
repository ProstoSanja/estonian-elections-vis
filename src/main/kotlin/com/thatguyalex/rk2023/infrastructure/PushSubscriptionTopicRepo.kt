package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.push.PushSubscriptionTopic
import com.thatguyalex.rk2023.infrastructure.classes.push.PushTopicType
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PushSubscriptionTopicRepo : CrudRepository<PushSubscriptionTopic, Long> {
    
    fun findByPushSubscriptionId(subscriptionId: Long): List<PushSubscriptionTopic>
    
    fun findByElectionType(electionType: ElectionType): List<PushSubscriptionTopic>
    
    fun findByElectionTypeAndTopicTypeAndTopicCode(
        electionType: ElectionType,
        topicType: PushTopicType,
        topicCode: String
    ): List<PushSubscriptionTopic>
    
    @Modifying
    @Query("DELETE FROM push_subscriptions_topics WHERE push_subscription_id = :subscriptionId")
    fun deleteByPushSubscriptionId(subscriptionId: Long): Int
    
    @Query("""
        SELECT DISTINCT pst.push_subscription_id 
        FROM push_subscriptions_topics pst 
        WHERE pst.election_type = :electionType
        AND pst.topic_type = :topicType 
        AND pst.topic_code = :topicCode
    """)
    fun findSubscriptionIdsByTopic(
        electionType: ElectionType,
        topicType: PushTopicType,
        topicCode: String
    ): List<Long>
    
    @Query("""
        SELECT DISTINCT pst.push_subscription_id 
        FROM push_subscriptions_topics pst 
        WHERE pst.election_type = :electionType
    """)
    fun findSubscriptionIdsByElection(electionType: ElectionType): List<Long>
}

