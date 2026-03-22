package com.thatguyalex.monitoring.application

import com.thatguyalex.monitoring.infrastructure.mongo.*
import com.thatguyalex.monitoring.infrastructure.rest.*
import com.thatguyalex.monitoring.infrastructure.rest.AriregisterClient.Companion.NS
import com.thatguyalex.monitoring.infrastructure.rest.AriregisterClient.Companion.directChildElements
import com.thatguyalex.monitoring.infrastructure.rest.AriregisterClient.Companion.elements
import com.thatguyalex.monitoring.infrastructure.rest.AriregisterClient.Companion.textContent
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import java.time.LocalDate

@Service
class AriregisterSyncService(
    private val client: AriregisterClient,
    private val lookupService: MonitoringEntryLookupService,
    private val mongoTemplate: MongoTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // @EventListener(ApplicationReadyEvent::class)
    fun onStartup() {
        val entries = mongoTemplate.find(
            Query(Criteria.where("type").ne(MonitoringEntryType.INDIVIDUAL.name)
                .and("ids.estGovId").ne(null)),
            MonitoringEntry::class.java,
        )
        log.info("Starting ariregister sync for {} entries", entries.size)
        var success = 0
        var failed = 0
        for ((i, entry) in entries.withIndex()) {
            val code = entry.ids.estGovId ?: continue
            try {
                sync(code)
                success++
            } catch (e: Exception) {
                failed++
                log.error("Failed to sync {} ({}): {}", code, entry.name, e.message)
            }
            if ((i + 1) % 100 == 0) {
                log.info("Progress: {}/{} (success={}, failed={})", i + 1, entries.size, success, failed)
            }
        }
        log.info("Ariregister sync complete: {}/{} succeeded, {} failed", success, entries.size, failed)
    }

    // TODO: alex - do not update type for already existing entry (or create a more thorough mapping akin to import_1 using alaliiks)
    fun sync(registrikood: String) {
        val doc = client.fetchDetails(registrikood)

        val items = elements(doc.documentElement, "item")
        val ettevotja = items.firstOrNull {
            textContent(it, "ariregistri_kood") == registrikood
        } ?: throw IllegalStateException("Company element not found in response for $registrikood")

        val name = textContent(ettevotja, "nimi")
            ?: throw IllegalStateException("Company name missing for $registrikood")

        val yldandmed = elements(ettevotja, "yldandmed").firstOrNull()
        val legalForm = yldandmed?.let { textContent(it, "oiguslik_vorm") }
        val entryType = ARIREGISTER_LEGAL_FORM_TYPE_MAPPINGS[legalForm] ?: MonitoringEntryType.BUSINESS

        val result = lookupService.findOrCreate(
            LookupParams(individual = false, fullName = name, estGovId = registrikood)
        )
        if (result.entries.size != 1) {
            throw IllegalStateException(
                "Expected exactly 1 entry for org $registrikood ($name), got ${result.entries.size}"
            )
        }
        val entryId = result.entries[0].id!!

        updateCompanyEntry(entryId, yldandmed, if (result.created) entryType else null)

        val isikuandmed = elements(ettevotja, "isikuandmed").firstOrNull()
        if (isikuandmed != null) {
            processConnectedPersons(entryId, registrikood, isikuandmed, "kaardile_kantud_isikud")
            processConnectedPersons(entryId, registrikood, isikuandmed, "kaardivalised_isikud")
        }

        log.info("Sync complete for registrikood={} ({})", registrikood, name)
    }

    private fun updateCompanyEntry(
        entryId: ObjectId,
        yldandmed: Element?,
        entryType: MonitoringEntryType?,
    ) {
        val update = Update()
        if (entryType != null) {
            update.set("type", entryType)
        }

        if (yldandmed != null) {
            val altNames = buildAltNames(yldandmed)
            if (altNames.isNotEmpty()) {
                update.set("altNames", altNames)
            }

            val contacts = buildContacts(yldandmed)
            if (contacts.isNotEmpty()) {
                update.set("unstructuredData.contacts", contacts)
            }
        }

        mongoTemplate.updateFirst(
            Query(Criteria.where("_id").`is`(entryId)),
            update,
            MonitoringEntry::class.java,
        )
    }

    private fun buildAltNames(yldandmed: Element): List<MonitoringEntryNames> {
        val arinimedContainer = elements(yldandmed, "arinimed").firstOrNull() ?: return emptyList()
        return directChildElements(arinimedContainer, "item").mapNotNull { item ->
            val fullName = textContent(item, "sisu") ?: return@mapNotNull null
            val startDate = parseAriDate(textContent(item, "algus_kpv"))
            val endDate = parseAriDate(textContent(item, "lopp_kpv"))
            val (bizName, bizSuffix) = splitBusinessSuffix(fullName)
            MonitoringEntryNames(
                fullName = fullName,
                businessName = bizName,
                businessSuffix = bizSuffix,
                startDate = startDate,
                endDate = endDate,
            )
        }
    }

    private fun buildContacts(yldandmed: Element): List<Map<String, String>> {
        val container = elements(yldandmed, "sidevahendid").firstOrNull() ?: return emptyList()
        return directChildElements(container, "item").mapNotNull { item ->
            val liik = textContent(item, "liik") ?: return@mapNotNull null
            val sisu = textContent(item, "sisu") ?: return@mapNotNull null
            val type = ARIREGISTER_CONTACT_TYPE_MAPPINGS[liik] ?: "other"
            mapOf("type" to type, "value" to sisu)
        }
    }

    private fun processConnectedPersons(
        companyId: ObjectId,
        registrikood: String,
        isikuandmed: Element,
        containerTag: String,
    ) {
        val container = elements(isikuandmed, containerTag).firstOrNull() ?: return
        val items = directChildElements(container, "item")
        log.info("Processing {} {} entries for {}", items.size, containerTag, registrikood)

        for (item in items) {
            try {
                processOneConnection(companyId, registrikood, item)
            } catch (e: Exception) {
                val roll = textContent(item, "isiku_roll") ?: "?"
                val nimi = textContent(item, "nimi_arinimi") ?: "?"
                log.warn("Failed to process connection {} ({}) for {}: {}", nimi, roll, registrikood, e.message)
            }
        }
    }

    private fun processOneConnection(companyId: ObjectId, registrikood: String, item: Element) {
        val isikuTyyp = textContent(item, "isiku_tyyp")
        val roll = textContent(item, "isiku_roll") ?: return
        val rollText = textContent(item, "isiku_roll_tekstina") ?: roll

        val connectionType = ARIREGISTER_ROLE_MAPPINGS[roll]
        if (connectionType == null) {
            log.debug("Skipping unmapped role {} for {}", roll, registrikood)
            return
        }

        val relatedId = resolveRelatedEntry(item, isikuTyyp) ?: return
        val startDate = parseAriDate(textContent(item, "algus_kpv"))
        val endDate = parseAriDate(textContent(item, "lopp_kpv"))

        upsertConnection(companyId, relatedId, connectionType, rollText, startDate, endDate, registrikood)
    }

    private fun resolveRelatedEntry(item: Element, isikuTyyp: String?): ObjectId? {
        val code = textContent(item, "isikukood_registrikood")

        return if (isikuTyyp == "F") {
            val firstName = textContent(item, "eesnimi")
            val lastName = textContent(item, "nimi_arinimi")
            val fullName = listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { null }
            val result = lookupService.findOrCreate(
                LookupParams(
                    individual = true,
                    fullName = fullName,
                    estGovId = code,
                    firstName = firstName,
                    lastName = lastName,
                )
            )
            if (result.entries.size != 1) {
                throw IllegalStateException(
                    "Expected exactly 1 person for $firstName $lastName ($code), got ${result.entries.size}"
                )
            }
            result.entries[0].id
        } else {
            val orgName = textContent(item, "nimi_arinimi") ?: return null
            val result = lookupService.findOrCreate(
                LookupParams(individual = false, fullName = orgName, estGovId = code)
            )
            if (result.entries.size != 1) {
                throw IllegalStateException(
                    "Expected exactly 1 org for $orgName ($code), got ${result.entries.size}"
                )
            }
            result.entries[0].id
        }
    }

    private fun upsertConnection(
        companyId: ObjectId,
        relatedId: ObjectId,
        type: MonitoringEntryConnectionType,
        name: String,
        startDate: LocalDate?,
        endDate: LocalDate?,
        registrikood: String,
    ) {
        val ids = listOf(companyId, relatedId)

        val criteria = Criteria.where("connectedIds").all(ids)
            .and("type").`is`(type)
            .and("startDate").`is`(startDate)

        val candidates = mongoTemplate.find(Query(criteria), MonitoringEntryConnection::class.java)

        val existing = when {
            candidates.size == 1 -> candidates[0]
            candidates.size > 1 -> candidates.firstOrNull { it.name == name }
            else -> null
        }

        if (existing != null) {
            val update = Update()
            if (!existing.confirmed) {
                update.set("confirmed", true)
            }
            if (existing.endDate != endDate) {
                update.set("endDate", endDate)
            }
            if (existing.connectedIds.toSet() != ids.toSet()) {
                update.set("connectedIds", ids)
            }
            if (update.updateObject.isNotEmpty()) {
                mongoTemplate.updateFirst(
                    Query(Criteria.where("_id").`is`(existing.id)),
                    update,
                    MonitoringEntryConnection::class.java,
                )
                log.debug("Updated connection {} for {}", existing.id, registrikood)
            }
            return
        }

        mongoTemplate.insert(
            MonitoringEntryConnection(
                connectedIds = ids,
                parentId = if (type == MonitoringEntryConnectionType.BUSINESS_OWNERSHIP) relatedId else null,
                name = name,
                type = type,
                confirmed = true,
                startDate = startDate,
                endDate = endDate,
                sources = listOf(
                    MonitoringEntryConnectionSource(
                        sourceUrl = "https://ariregister.rik.ee/est/company/$registrikood"
                    )
                ),
            )
        )
        log.debug("Created connection {} ({}) for {}", name, type, registrikood)
    }

    companion object {
        fun parseAriDate(value: String?): LocalDate? {
            if (value.isNullOrBlank()) return null
            val clean = value.removeSuffix("Z")
            return try {
                LocalDate.parse(clean)
            } catch (_: Exception) {
                null
            }
        }

        fun splitBusinessSuffix(fullName: String): Pair<String, String?> {
            for (suffix in ARIREGISTER_LEGAL_FORM_SUFFIXES) {
                if (fullName.startsWith("$suffix ", ignoreCase = true)) {
                    return fullName.removePrefix(suffix).trim() to suffix
                }
                if (fullName.endsWith(" $suffix", ignoreCase = true)) {
                    return fullName.removeSuffix(suffix).trim() to suffix
                }
            }
            return fullName to null
        }
    }
}
