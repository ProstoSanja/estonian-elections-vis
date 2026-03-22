package com.thatguyalex.monitoring.infrastructure.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.bson.types.ObjectId
import java.time.Instant
import java.time.LocalDate

@Document(collection = "monitoring_entry_connections")
data class MonitoringEntryConnection(
    @Id
    val id: ObjectId? = null,
    @Indexed
    val connectedIds: List<ObjectId>,
    val parentId: ObjectId? = null, // only used for strict relationship, org structure
    val name: String,
    val type: MonitoringEntryConnectionType,
    val confirmed: Boolean,
    val monetaryValue: Double? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    // backup raw import data
    val sources: List<MonitoringEntryConnectionSource>,
)

enum class MonitoringEntryConnectionType {
    PARTY_MEMBERSHIP,
    BUSINESS_OWNERSHIP,
    EMPLOYMENT,
    ADMINISTRATION,
    DONATION,
    BANK_LOAN,
    // and unknown
    UNKNOWN,
}

data class MonitoringEntryConnectionSource(
    val sourceUrl: String,
)

interface MonitoringEntryConnectionRepository : MongoRepository<MonitoringEntryConnection, String>
