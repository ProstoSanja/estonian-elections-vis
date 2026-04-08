package com.thatguyalex.monitoring.infrastructure.jdbc

import com.thatguyalex.monitoring.infrastructure.MonitoringEntryType
import com.thatguyalex.monitoring.infrastructure.MonitoringExternalIdType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("monitoring_entry")
data class MonitoringEntryEntity(
    @Id val id: Long? = null,
    val type: MonitoringEntryType,
    val name: String,
    val birthDate: LocalDate? = null,
    val extra: Jsonb? = null,
)

@Table("monitoring_entry_external_id")
data class MonitoringEntryExternalIdEntity(
    @Id val id: Long? = null,
    val entryId: Long,
    val type: MonitoringExternalIdType,
    val value: String,
)

@Table("monitoring_entry_name")
data class MonitoringEntryNameEntity(
    @Id val id: Long? = null,
    val entryId: Long,
    val fullName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val businessName: String? = null,
    val businessSuffix: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
