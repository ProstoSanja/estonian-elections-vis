package com.thatguyalex.monitoring.application

import com.thatguyalex.monitoring.infrastructure.mongo.*
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.regex.Pattern

data class LookupParams(
    val individual: Boolean,
    val fullName: String? = null,
    val estGovId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    private val rawBirthDate: LocalDate? = null,
) {
    val birthDate: LocalDate? = resolveBirthDate()

    private fun resolveBirthDate(): LocalDate? {
        if (!individual || estGovId.isNullOrBlank() || estGovId.length < 7) return rawBirthDate
        val firstDigit = estGovId[0].digitToIntOrNull() ?: return rawBirthDate
        val century = when (firstDigit) {
            1, 2 -> 1800
            3, 4 -> 1900
            5, 6 -> 2000
            else -> return rawBirthDate
        }
        val year = estGovId.substring(1, 3).toIntOrNull() ?: return rawBirthDate
        val month = estGovId.substring(3, 5).toIntOrNull() ?: return rawBirthDate
        val day = estGovId.substring(5, 7).toIntOrNull() ?: return rawBirthDate
        return try {
            LocalDate.of(century + year, month, day)
        } catch (_: Exception) {
            rawBirthDate
        }
    }
}

data class EntryLookupResult(
    val entries: List<MonitoringEntry>,
    val created: Boolean,
    val updatedBirthdate: Boolean,
)

@Service
class MonitoringEntryLookupService(
    private val mongoTemplate: MongoTemplate,
) {
    fun findOrCreate(params: LookupParams): EntryLookupResult {
        val hasEstGovId = !params.estGovId.isNullOrBlank()
        val hasNameParts = !params.firstName.isNullOrBlank() && !params.lastName.isNullOrBlank()
        val hasFullName = !params.fullName.isNullOrBlank()

        require(hasEstGovId || hasNameParts || hasFullName) {
            "At least one lookup path required: estGovId, firstName+lastName, or fullName"
        }

        val found = lookup(params, hasEstGovId, hasNameParts, hasFullName)

        return if (params.individual) {
            disambiguateIndividual(found, params)
        } else {
            handleOrg(found, params)
        }
    }

    private fun lookup(
        params: LookupParams,
        hasEstGovId: Boolean,
        hasNameParts: Boolean,
        hasFullName: Boolean,
    ): List<MonitoringEntry> {
        if (hasEstGovId) {
            val found = mongoTemplate.find(
                Query(Criteria.where("ids.estGovId").`is`(params.estGovId)),
                MonitoringEntry::class.java,
            )
            if (found.isNotEmpty()) return found
        }

        if (hasNameParts) {
            val criteria = typeCriteria(params.individual)
                .and("altNames.firstName").regex("^${Pattern.quote(params.firstName!!.trim())}$", "i")
                .and("altNames.lastName").regex("^${Pattern.quote(params.lastName!!.trim())}$", "i")
            val found = mongoTemplate.find(Query(criteria), MonitoringEntry::class.java)
            if (found.isNotEmpty()) return found
        }

        if (hasFullName) {
            val criteria = typeCriteria(params.individual)
                .and("name").regex("^${Pattern.quote(params.fullName!!.trim())}$", "i")
            return mongoTemplate.find(Query(criteria), MonitoringEntry::class.java)
        }

        return emptyList()
    }

    private fun typeCriteria(individual: Boolean): Criteria =
        if (individual) Criteria.where("type").`is`(MonitoringEntryType.INDIVIDUAL.name)
        else Criteria.where("type").ne(MonitoringEntryType.INDIVIDUAL.name)

    private fun disambiguateIndividual(found: List<MonitoringEntry>, params: LookupParams): EntryLookupResult {
        if (found.isEmpty()) {
            val created = createIndividual(params)
            return EntryLookupResult(listOf(created), created = true, updatedBirthdate = false)
        }

        val birthDate = params.birthDate
            ?: return EntryLookupResult(found, created = false, updatedBirthdate = false)

        if (found.size == 1) {
            val rec = found[0]
            if (rec.birthDate == null || rec.birthDate == birthDate) {
                val patched = patchIndividual(rec, params)
                return EntryLookupResult(listOf(patched), created = false, updatedBirthdate = rec.birthDate != birthDate)
            }
            val created = createIndividual(params)
            return EntryLookupResult(listOf(created), created = true, updatedBirthdate = false)
        }

        val bdMatch = found.filter { it.birthDate == birthDate }
        if (bdMatch.isNotEmpty()) {
            return EntryLookupResult(bdMatch, created = false, updatedBirthdate = false)
        }

        val noBd = found.filter { it.birthDate == null }
        if (noBd.size == 1) {
            val patched = patchIndividual(noBd[0], params)
            return EntryLookupResult(listOf(patched), created = false, updatedBirthdate = true)
        }
        if (noBd.size > 1) {
            return EntryLookupResult(noBd, created = false, updatedBirthdate = false)
        }

        val created = createIndividual(params)
        return EntryLookupResult(listOf(created), created = true, updatedBirthdate = false)
    }

    private fun handleOrg(found: List<MonitoringEntry>, params: LookupParams): EntryLookupResult {
        if (found.size > 1) {
            val names = found.map { it.name }
            throw IllegalStateException("Multiple entries found for org lookup: $names")
        }
        if (found.size == 1) {
            return EntryLookupResult(found, created = false, updatedBirthdate = false)
        }

        val name = requireNotNull(params.fullName?.trim()) {
            "fullName is required to create a new org entry"
        }
        val entry = mongoTemplate.insert(
            MonitoringEntry(
                ids = MonitoringEntryIds(estGovId = params.estGovId),
                name = name,
                type = MonitoringEntryType.BUSINESS,
                altNames = listOf(MonitoringEntryNames(fullName = name, businessName = name, businessSuffix = null)),
            )
        )
        return EntryLookupResult(listOf(entry), created = true, updatedBirthdate = false)
    }

    private fun patchIndividual(entry: MonitoringEntry, params: LookupParams): MonitoringEntry {
        val update = Update()
        var patched = entry

        if (entry.birthDate == null && params.birthDate != null) {
            update.set("birthDate", params.birthDate)
            patched = patched.copy(birthDate = params.birthDate)
        }
        if (entry.altNames.size == 1 && !params.firstName.isNullOrBlank() && !params.lastName.isNullOrBlank()) {
            update.set("altNames.0.fullName", entry.name.trim())
            update.set("altNames.0.firstName", params.firstName.trim())
            update.set("altNames.0.lastName", params.lastName.trim())
            patched = patched.copy(altNames = listOf(MonitoringEntryNames(
                fullName = entry.name.trim(),
                firstName = params.firstName.trim(),
                lastName = params.lastName.trim(),
            )))
        } else if (entry.altNames.isEmpty() && !params.firstName.isNullOrBlank() && !params.lastName.isNullOrBlank()) {
            val newName = MonitoringEntryNames(
                fullName = entry.name.trim(),
                firstName = params.firstName.trim(),
                lastName = params.lastName.trim(),
            )
            update.set("altNames", listOf(newName))
            patched = patched.copy(altNames = listOf(newName))
        }
        if (entry.ids.estGovId == null && !params.estGovId.isNullOrBlank()) {
            update.set("ids.estGovId", params.estGovId)
            patched = patched.copy(ids = patched.ids.copy(estGovId = params.estGovId))
        }

        if (update.updateObject.isNotEmpty()) {
            mongoTemplate.updateFirst(
                Query(Criteria.where("_id").`is`(entry.id)),
                update,
                MonitoringEntry::class.java,
            )
        }
        return patched
    }

    private fun createIndividual(params: LookupParams): MonitoringEntry {
        val name = params.fullName?.trim()
            ?: listOfNotNull(params.firstName?.trim(), params.lastName?.trim()).joinToString(" ")

        return mongoTemplate.insert(
            MonitoringEntry(
                ids = MonitoringEntryIds(estGovId = params.estGovId),
                name = name,
                type = MonitoringEntryType.INDIVIDUAL,
                altNames = listOf(MonitoringEntryNames(
                    fullName = name,
                    firstName = params.firstName?.trim(),
                    lastName = params.lastName?.trim(),
                )),
                birthDate = params.birthDate,
            )
        )
    }
}
