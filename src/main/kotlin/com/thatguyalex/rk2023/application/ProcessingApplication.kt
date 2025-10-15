package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.classes.*
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ProcessingApplication {

    companion object {
        private val partySort = compareBy<Party>{ it.name.lowercase().contains("liit") }.thenBy { it.name.lowercase().trim() }
    }

    fun process(rawResults: ElectionResultsData, fallback: ProcessedResults?): ProcessedResults {
        return when (rawResults) {
            is RK2ResultsData -> processRK2(rawResults.electionResult)
            is KOV2ResultsData -> processKOV2(rawResults, fallback)
            is CAND1ResultsData -> processCAND1(rawResults)
            else -> throw IllegalArgumentException("unknown results type to be processed")
        }
    }

    private fun processKOV2(rawResults: KOV2ResultsData, fallback: ProcessedResults?): ProcessedResults {
        val candidates = rawResults
            .flatMap { it.votesAndMandates.map { party -> it.adminUnit.ehakCode.toInt() to party  } }
            .flatMap { (ehakCode, party) ->  party.candidates.map { cand -> cand.toResult(party.code ?: "ÜKSIK", ehakCode) } }
            .sortedByDescending { it.votes }
            .ifEmpty { fallback?.candidates ?: emptyList() }
        val districts = rawResults.map { it.toResult(rawResults) }
        val parties = rawResults
            .flatMap { it.votesAndMandates }
            .groupBy { it.code ?: "ÜKSIK" }
            .map {
                it.value.kov2ListToResult()
            }
            .ifEmpty { fallback?.parties ?: emptyList() }
            .sortedWith(partySort)
        return ProcessedResults(parties, districts, candidates)
    }

    private fun processRK2(rawResults: RK2Result): ProcessedResults {
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand.toResult(it.code ?: "ÜKSIK") } }
            .sortedByDescending { it.votes }
        val globalDistrict = rawResults.toResult()
        val districts = rawResults.districts.map { it.toResult(rawResults.parties.flatMap { it.candidates }) }
            .plus(globalDistrict)
        return ProcessedResults(globalDistrict.parties, districts, candidates)
    }

    private fun processCAND1(rawResults: CAND1ResultsData): ProcessedResults {
        // KOV Edition. For RK top level different?
        val candidates = rawResults.adminUnits.flatMap { unpackCAND1(it) { adminUnit ->
            adminUnit.districts
                .flatMap { district -> district.parties
                    .flatMap { party -> party.candidates
                        .map { candidate -> candidate.toResult(party.partyCode, adminUnit.ehakCode.toInt()) } // district.districtNumber is for tallinn, it is mostly 1 for all others
                    }
                }
        } }.sortedBy { Random.nextLong() }
        val parties = rawResults.adminUnits.flatMap { unpackCAND1(it) { adminUnit ->
            adminUnit.districts.flatMap { district -> district.parties }
        } }
            .groupBy { party -> party.partyCode }
            .map {
                it.value.cand1ListtoResult()
            }
            .sortedWith(partySort)
        return ProcessedResults(parties, emptyList(), candidates)
    }

    private fun <T> unpackCAND1(cand1AdminUnit: CAND1AdminUnit, ret: (cand1AdminUnit: CAND1AdminUnit) -> List<T>): List<T> {
        val directResult = ret(cand1AdminUnit)
        val childResults = if (cand1AdminUnit.childAdminUnits.isNotEmpty()) cand1AdminUnit.childAdminUnits.flatMap { unpackCAND1(it, ret) } else emptyList()
        return directResult + childResults
    }
}