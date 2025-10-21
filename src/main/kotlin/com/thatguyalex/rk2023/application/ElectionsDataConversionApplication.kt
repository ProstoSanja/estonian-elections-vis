package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.classes.elections.Party
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.classes.elections.CAND1AdminUnit
import com.thatguyalex.rk2023.infrastructure.classes.elections.CAND1ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.Candidate
import com.thatguyalex.rk2023.infrastructure.classes.elections.District
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV1MUNElectionResult
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV1MUNResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV2ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.RK2Result
import com.thatguyalex.rk2023.infrastructure.classes.elections.RK2ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.cand1ListtoResult
import com.thatguyalex.rk2023.infrastructure.classes.elections.getTotalVotes
import com.thatguyalex.rk2023.infrastructure.classes.elections.kov2ListToResult
import com.thatguyalex.rk2023.infrastructure.classes.elections.syntheticSubDistrictCodeFor
import com.thatguyalex.rk2023.infrastructure.classes.elections.toResult
import com.thatguyalex.rk2023.infrastructure.classes.helpers.tokenizeString
import org.springframework.stereotype.Service

@Service
class ElectionsDataConversionApplication {

    companion object {
        val partySort = compareBy<Party>{ it.name.lowercase().contains("liit") }.thenBy { it.name.lowercase().trim() }
        val candidateSort = compareByDescending<Candidate> { it.votes }.thenBy { it.forename }.thenBy { it.surename }
        val districtSort = compareBy<District> { district ->
            val nameLower = district.name.lowercase()
            when {
                nameLower.contains("linnaosa") -> 1 // must come before tallinn, because "põhja-tallinn matches"
                nameLower.contains("tallinn") -> 0
                nameLower.contains("linn") -> 2
                nameLower.contains("maakond") -> 4
                else -> 3
            }
        }.thenBy { it.name.lowercase() }
    }

    fun process(rawResults: GOVResultsData, candidates: ProcessedResults?, extraParam: String? = null): ProcessedResults {
        return when (rawResults) {
            is RK2ResultsData -> processRK2(rawResults.electionResult)
            is KOV2ResultsData -> processKOV2(rawResults)
            is CAND1ResultsData -> processCAND1(rawResults)
            is KOV1MUNResultsData -> processKOV1MUN(rawResults.electionResult, candidates, extraParam!!.toInt())
            else -> throw IllegalArgumentException("unknown results type to be processed")
        }
    }

    private fun processKOV2(rawResults: KOV2ResultsData): ProcessedResults {
        val candidates = rawResults
            .flatMap { it.votesAndMandates.map { party -> Triple(it.adminUnit.ehakCode.toInt(), it.adminUnit.parentEhakCode?.toInt(), party) } }
            .flatMap { (ehakCode, parentEhakCode, party) ->  party.candidates.map { cand ->
                val districtCode = if (ehakCode == 784) ehakCode.syntheticSubDistrictCodeFor(cand.districtNumber) else null
                cand.toResult(party.code ?: "ÜKSIK", party.name,ehakCode, listOfNotNull(ehakCode, parentEhakCode, districtCode))
            } }
        val districts = rawResults.map { it.toResult(rawResults) }
        val parties = rawResults
            .flatMap { it.votesAndMandates }
            .groupBy { (it.code ?: "ÜKSIK") to tokenizeString(it.name) }
            .map { it.value.kov2ListToResult() }
        return ProcessedResults(parties, districts, candidates)
    }

    private fun processRK2(rawResults: RK2Result): ProcessedResults {
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand.toResult(it.code ?: "ÜKSIK", it.name ?: "Üksikkandidaadid") } }
        val globalDistrict = rawResults.toResult()
        val districts = rawResults.districts.map { it.toResult(rawResults.parties.flatMap { it.candidates }) }
            .plus(globalDistrict)
        return ProcessedResults(globalDistrict.parties, districts, candidates)
    }

    private fun processCAND1(rawResults: CAND1ResultsData): ProcessedResults {
        // TODO: KOV Edition. For RK top level different?
        val candidates = unpackCAND1(rawResults.adminUnits) { adminUnit, parentUnits ->
            adminUnit.districts
                .flatMap { district -> district.parties
                    .flatMap { party -> party.candidates
                        .map { candidate ->
                            val ehakCode = adminUnit.ehakCode.toInt()
                            val districtCode = if (ehakCode == 784) ehakCode.syntheticSubDistrictCodeFor(district.districtNumber) else null
                            val allCodes = parentUnits.map { parent -> parent.ehakCode.toInt() } + ehakCode + districtCode + 0
                            candidate.toResult(party.partyCode, party.partyName,ehakCode, allCodes.filterNotNull())
                        }
                    }
                }
        }
        val parties = unpackCAND1(rawResults.adminUnits) { adminUnit, _ ->
            adminUnit.districts.flatMap { district -> district.parties }
        }
            .groupBy { party -> party.partyCode to tokenizeString(party.partyName) } // partyCode turns out is not unique, holy shit
            .map { it.value.cand1ListtoResult() }
        val districts = unpackCAND1(rawResults.adminUnits) { adminUnit, _ ->
            adminUnit.districts
                .map { district -> district.toResult(adminUnit.ehakCode.toInt()) }
                .takeIf { districts -> districts.size > 1 } ?: emptyList()
        }
        return ProcessedResults(parties, districts, candidates)
    }

    private fun <T> unpackCAND1(adminUnits: List<CAND1AdminUnit>, parentUnits: List<CAND1AdminUnit> = emptyList(), ret: (cand1AdminUnit: CAND1AdminUnit, parentUnits: List<CAND1AdminUnit>) -> List<T>): List<T> {
        if (adminUnits.isEmpty()) return emptyList()
        return adminUnits
            .associateWith { parentUnits + it }
            .flatMap { (adminUnit, parentUnitsWithCurrent) -> ret(adminUnit, parentUnitsWithCurrent) + unpackCAND1(adminUnit.childAdminUnits, parentUnitsWithCurrent, ret) }
    }

    private fun processKOV1MUN(rawResults: KOV1MUNElectionResult, fallback: ProcessedResults?, parentRegion: Int): ProcessedResults {
        val districts = rawResults.districts.map { district -> district.toResult(parentRegion, fallback) }
        val candidates = rawResults.districts.flatMap { district ->
            district.voteDistributionByParties.flatMap { party ->
                party.candidates.mapNotNull { candidate ->
                    fallback?.candidates[Candidate.buildUniqueId(parentRegion, candidate.candidateRegNumber)]?.copy(votes = candidate.votesDistributionRow.getTotalVotes())
                }
            }
        }
        return ProcessedResults(emptyList(), districts, candidates)
    }
}