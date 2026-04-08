package com.thatguyalex.monitoring.infrastructure.jdbc

import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate

@Table("monitoring_connection")
data class MonitoringConnectionEntity(
    @Id val id: Long? = null,
    val type: MonitoringEntryConnectionType,
    val name: String,
    val confirmed: Boolean = false,
    val entryAId: Long,
    val entryBId: Long? = null,
    val parentId: Long? = null,
    val monetaryValue: BigDecimal? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val sources: Jsonb? = null,
)

@Table("monitoring_connection_candidate")
data class MonitoringConnectionCandidateEntity(
    val connectionId: Long,
    val entryId: Long,
)
