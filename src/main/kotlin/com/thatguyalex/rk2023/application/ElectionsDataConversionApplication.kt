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
import com.thatguyalex.rk2023.infrastructure.classes.elections.VoteStats
import com.thatguyalex.rk2023.infrastructure.classes.elections.cand1ListtoResult
import com.thatguyalex.rk2023.infrastructure.classes.elections.getEVotes
import com.thatguyalex.rk2023.infrastructure.classes.elections.getTotalVotes
import com.thatguyalex.rk2023.infrastructure.classes.elections.kov2ListToResult
import com.thatguyalex.rk2023.infrastructure.classes.elections.toResult
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ElectionsDataConversionApplication {

    companion object {
        val partySort = compareBy<Party>{ it.name.lowercase().contains("liit") }.thenBy { it.name.lowercase().trim() }
        val candidateSort = compareByDescending<Candidate> { it.votes }.thenBy { it.forename }.thenBy { it.surename }
        val districtSort = compareBy<District> { district ->
            val nameLower = district.name.lowercase()
            when {
                nameLower.contains("linnaosa") -> 1
                nameLower.contains("tallinn") || nameLower.contains("tartu linn") -> 0
                nameLower.contains("linn") -> 2
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
            .flatMap { it.votesAndMandates.map { party -> it.adminUnit.ehakCode.toInt() to party  } }
            .flatMap { (ehakCode, party) ->  party.candidates.map { cand -> cand.toResult(party.code ?: "ÜKSIK", ehakCode) } }
        val districts = rawResults.map { it.toResult(rawResults) }
        val parties = rawResults
            .flatMap { it.votesAndMandates }
            .groupBy { it.code ?: "ÜKSIK" }
            .map { it.value.kov2ListToResult() }
        return ProcessedResults(parties, districts, candidates)
    }

    private fun processRK2(rawResults: RK2Result): ProcessedResults {
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand.toResult(it.code ?: "ÜKSIK") } }
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
                        // TODO: populate candidate region allegiances when switched to array
                        .map { candidate -> candidate.toResult(party.partyCode, adminUnit.ehakCode.toInt()) } // district.districtNumber is for tallinn, it is mostly 1 for all others
                    }
                }
        } }
        val parties = rawResults.adminUnits.flatMap { unpackCAND1(it) { adminUnit ->
            adminUnit.districts.flatMap { district -> district.parties }
        } }
            .groupBy { party -> party.partyCode }
            .map {
                it.value.cand1ListtoResult()
            }
        val districts = rawResults.adminUnits.flatMap { unpackCAND1(it) { adminUnit ->
            adminUnit.districts
                .map { district ->
                    District(
                        name = district.districtComment
                            .replace(Regex("[–-]"), "-")
                            .split("-")
                            .drop(1)
                            .joinToString("-")
                            .trim(),
                        number = (adminUnit.ehakCode.toInt() * 10) + district.districtNumber,
                        parties = emptyList(),
                        voteStats = VoteStats.empty(),
                        totalMandates = 0
                    )
                }
                .takeIf { districts -> districts.size > 1 } ?: emptyList()
        } }
        return ProcessedResults(parties, districts, candidates)
    }

    private fun <T> unpackCAND1(cand1AdminUnit: CAND1AdminUnit, ret: (cand1AdminUnit: CAND1AdminUnit) -> List<T>): List<T> {
        val directResult = ret(cand1AdminUnit)
        val childResults = if (cand1AdminUnit.childAdminUnits.isNotEmpty()) cand1AdminUnit.childAdminUnits.flatMap { unpackCAND1(it, ret) } else emptyList()
        return directResult + childResults
    }

    private fun processKOV1MUN(rawResults: KOV1MUNElectionResult, fallback: ProcessedResults?, parentRegion: Int): ProcessedResults {
        val districts = rawResults.districts.map { district ->
            val districtNumber = (parentRegion * 10) + district.districtNumber
            District(
                name = fallback?.districts[districtNumber]?.name ?: "Valimisrigkond $districtNumber",
                number = districtNumber,
                parties = district.voteDistributionByParties.map { party ->
                    Party(
                        name = party.name,
                        code = fallback?.partiesList?.firstOrNull { it.name == party.name }?.code ?: "WTF",
                        mandates = party.candidates.count { it.elected },
                        votes = party.votesDistributionRow.getTotalVotes()
                    )
                }.sortedByDescending { it.votes },
                voteStats = VoteStats(
                    votesCounted = district.votesDistributionRow.getTotalVotes(),
                    protocolsCounted = district.votesDistributionRow.count { it.name.contains("J") && (it.value?.toInt() ?: 0) > 0 },
                    protocolsTotal = district.votesDistributionRow.count { it.name.contains("J") },
                    evotesCounted = district.votesDistributionRow.getEVotes() > 0
                ),
                totalMandates = district.mandateCount ?: 0
            )
        }

        return ProcessedResults(emptyList(), districts, emptyList())
    }
}