package com.thatguyalex.monitoring.infrastructure.jdbc

import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType
import com.thatguyalex.monitoring.infrastructure.MonitoringExternalIdType
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface MonitoringEntryRepo : CrudRepository<MonitoringEntryEntity, Long> {

    @Query("""
        SELECT me.* FROM monitoring_entry me
        JOIN monitoring_entry_external_id eid ON eid.entry_id = me.id
        WHERE me.type != 'INDIVIDUAL' AND eid.type = 'EST_GOV_ID'
    """)
    fun findNonIndividualWithEstGovId(): List<MonitoringEntryEntity>

    @Query("SELECT * FROM monitoring_entry WHERE lower(name) = lower(:name) AND type = 'INDIVIDUAL'")
    fun findIndividualByNameIgnoreCase(name: String): List<MonitoringEntryEntity>

    @Query("SELECT * FROM monitoring_entry WHERE lower(name) = lower(:name) AND type != 'INDIVIDUAL'")
    fun findOrgByNameIgnoreCase(name: String): List<MonitoringEntryEntity>

    @Modifying
    @Query("UPDATE monitoring_entry SET extra = CAST(:extra AS jsonb) WHERE id = :id")
    fun updateExtra(id: Long, extra: String)
}

@Repository
interface MonitoringEntryExternalIdRepo : CrudRepository<MonitoringEntryExternalIdEntity, Long> {

    fun findByEntryId(entryId: Long): List<MonitoringEntryExternalIdEntity>

    fun findByTypeAndValue(type: MonitoringExternalIdType, value: String): MonitoringEntryExternalIdEntity?

    @Query("""
        SELECT me.* FROM monitoring_entry me
        JOIN monitoring_entry_external_id eid ON eid.entry_id = me.id
        WHERE eid.type = :type AND eid.value = :value
    """)
    fun findEntriesByExternalId(type: MonitoringExternalIdType, value: String): List<MonitoringEntryEntity>
}

@Repository
interface MonitoringEntryNameRepo : CrudRepository<MonitoringEntryNameEntity, Long> {

    fun findByEntryId(entryId: Long): List<MonitoringEntryNameEntity>

    @Query("""
        SELECT DISTINCT me.* FROM monitoring_entry me
        JOIN monitoring_entry_name men ON men.entry_id = me.id
        WHERE lower(men.first_name) = lower(:firstName)
          AND lower(men.last_name) = lower(:lastName)
          AND me.type = 'INDIVIDUAL'
    """)
    fun findIndividualsByNameParts(firstName: String, lastName: String): List<MonitoringEntryEntity>

    @Modifying
    @Query("DELETE FROM monitoring_entry_name WHERE entry_id = :entryId")
    fun deleteByEntryId(entryId: Long): Int
}

@Repository
interface MonitoringConnectionRepo : CrudRepository<MonitoringConnectionEntity, Long> {

    @Query("""
        SELECT * FROM monitoring_connection
        WHERE type = :type
          AND start_date IS NOT DISTINCT FROM :startDate
          AND ((entry_a_id = :idA AND entry_b_id = :idB)
            OR (entry_a_id = :idB AND entry_b_id = :idA))
    """)
    fun findByPairAndTypeAndStartDate(
        idA: Long,
        idB: Long,
        type: MonitoringEntryConnectionType,
        startDate: LocalDate?,
    ): List<MonitoringConnectionEntity>
}

@Repository
interface MonitoringConnectionCandidateRepo : CrudRepository<MonitoringConnectionCandidateEntity, Long> {

    fun findByConnectionId(connectionId: Long): List<MonitoringConnectionCandidateEntity>

    fun findByEntryId(entryId: Long): List<MonitoringConnectionCandidateEntity>

    @Modifying
    @Query("DELETE FROM monitoring_connection_candidate WHERE connection_id = :connectionId")
    fun deleteByConnectionId(connectionId: Long): Int
}
