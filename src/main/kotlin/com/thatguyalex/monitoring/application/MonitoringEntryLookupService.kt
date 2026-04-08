package com.thatguyalex.monitoring.application

import com.thatguyalex.monitoring.infrastructure.MonitoringEntryType
import com.thatguyalex.monitoring.infrastructure.MonitoringExternalIdType
import com.thatguyalex.monitoring.infrastructure.jdbc.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

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
    val entries: List<MonitoringEntryEntity>,
    val created: Boolean,
    val updatedBirthdate: Boolean,
)

@Service
class MonitoringEntryLookupService(
    private val entryRepo: MonitoringEntryRepo,
    private val externalIdRepo: MonitoringEntryExternalIdRepo,
    private val nameRepo: MonitoringEntryNameRepo,
) {
    @Transactional
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
    ): List<MonitoringEntryEntity> {
        if (hasEstGovId) {
            val found = externalIdRepo.findEntriesByExternalId(
                MonitoringExternalIdType.EST_GOV_ID, params.estGovId!!,
            )
            if (found.isNotEmpty()) return found
        }

        if (hasNameParts) {
            val found = nameRepo.findIndividualsByNameParts(
                params.firstName!!.trim(), params.lastName!!.trim(),
            )
            if (found.isNotEmpty()) return found
        }

        if (hasFullName) {
            return if (params.individual) {
                entryRepo.findIndividualByNameIgnoreCase(params.fullName!!.trim())
            } else {
                entryRepo.findOrgByNameIgnoreCase(params.fullName!!.trim())
            }
        }

        return emptyList()
    }

    private fun disambiguateIndividual(found: List<MonitoringEntryEntity>, params: LookupParams): EntryLookupResult {
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

    private fun handleOrg(found: List<MonitoringEntryEntity>, params: LookupParams): EntryLookupResult {
        if (found.size > 1) {
            val names = found.map { it.name }
            throw IllegalStateException("Multiple entries found for org lookup: $names")
        }
        if (found.size == 1) {
            return EntryLookupResult(found, created = false, updatedBirthdate = false)
        }

        val entry = createOrg(params)
        return EntryLookupResult(listOf(entry), created = true, updatedBirthdate = false)
    }

    private fun createOrg(params: LookupParams): MonitoringEntryEntity {
        val name = requireNotNull(params.fullName?.trim()) {
            "fullName is required to create a new org entry"
        }

        val entry = entryRepo.save(MonitoringEntryEntity(
            type = MonitoringEntryType.BUSINESS,
            name = name,
        ))

        nameRepo.save(MonitoringEntryNameEntity(
            entryId = entry.id!!,
            fullName = name,
            businessName = name,
        ))

        if (!params.estGovId.isNullOrBlank()) {
            externalIdRepo.save(MonitoringEntryExternalIdEntity(
                entryId = entry.id,
                type = MonitoringExternalIdType.EST_GOV_ID,
                value = params.estGovId,
            ))
        }

        return entry
    }

    private fun patchIndividual(entry: MonitoringEntryEntity, params: LookupParams): MonitoringEntryEntity {
        var patched = entry

        if (entry.birthDate == null && params.birthDate != null) {
            patched = entryRepo.save(patched.copy(birthDate = params.birthDate))
        }

        if (!params.firstName.isNullOrBlank() && !params.lastName.isNullOrBlank()) {
            val existingNames = nameRepo.findByEntryId(entry.id!!)
            if (existingNames.size == 1) {
                nameRepo.save(existingNames[0].copy(
                    fullName = entry.name.trim(),
                    firstName = params.firstName.trim(),
                    lastName = params.lastName.trim(),
                ))
            } else if (existingNames.isEmpty()) {
                nameRepo.save(MonitoringEntryNameEntity(
                    entryId = entry.id,
                    fullName = entry.name.trim(),
                    firstName = params.firstName.trim(),
                    lastName = params.lastName.trim(),
                ))
            }
        }

        if (!params.estGovId.isNullOrBlank()) {
            val existing = externalIdRepo.findByTypeAndValue(
                MonitoringExternalIdType.EST_GOV_ID, params.estGovId,
            )
            if (existing == null) {
                externalIdRepo.save(MonitoringEntryExternalIdEntity(
                    entryId = entry.id!!,
                    type = MonitoringExternalIdType.EST_GOV_ID,
                    value = params.estGovId,
                ))
            }
        }

        return patched
    }

    private fun createIndividual(params: LookupParams): MonitoringEntryEntity {
        val name = params.fullName?.trim()
            ?: listOfNotNull(params.firstName?.trim(), params.lastName?.trim()).joinToString(" ")

        val entry = entryRepo.save(MonitoringEntryEntity(
            type = MonitoringEntryType.INDIVIDUAL,
            name = name,
            birthDate = params.birthDate,
        ))

        nameRepo.save(MonitoringEntryNameEntity(
            entryId = entry.id!!,
            fullName = name,
            firstName = params.firstName?.trim(),
            lastName = params.lastName?.trim(),
        ))

        if (!params.estGovId.isNullOrBlank()) {
            externalIdRepo.save(MonitoringEntryExternalIdEntity(
                entryId = entry.id,
                type = MonitoringExternalIdType.EST_GOV_ID,
                value = params.estGovId,
            ))
        }

        return entry
    }
}
