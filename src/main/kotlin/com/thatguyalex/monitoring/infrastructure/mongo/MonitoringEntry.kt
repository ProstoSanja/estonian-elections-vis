package com.thatguyalex.monitoring.infrastructure.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.bson.types.ObjectId
import java.time.Instant
import java.time.LocalDate

interface MonitoringEntryRepository : MongoRepository<MonitoringEntry, String>

@Document(collection = "monitoring_entries")
data class MonitoringEntry(
    @Id
    val id: ObjectId? = null,
    val ids: MonitoringEntryIds,
    @Indexed
    val name: String,
    val type: MonitoringEntryType,
    val nameParts: MonitoringEntryNameParts,
    @Indexed
    val birthDate: LocalDate? = null,
)

data class MonitoringEntryIds(
    @Indexed
    val estGovId: String? = null, // isikukood or ariregister
    val ariregisterAnonId: String? = null, // anonymized ariregister isikukood 9xxxxxxxx
)

data class MonitoringEntryNameParts(
    val firstName: String? = null,
    val lastName: String? = null,
    val businessName: String? = null,
    val businessSuffix: String? = null,
)

enum class MonitoringEntryType {
    INDIVIDUAL,
    PARTY,
    GOV,
    GOV_KOV,
    BUSINESS,
    PUBLIC_BODY,
}