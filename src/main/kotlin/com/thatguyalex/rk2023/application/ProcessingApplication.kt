package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.classes.*
import org.springframework.stereotype.Service

@Service
class ProcessingApplication {

    fun process(rawResults: ElectionResultsData): ProcessedResults {
        return when (rawResults) {
            is RK2ResultsData -> processRK2(rawResults.electionResult)
            is KOV2ResultsData -> processKOV2(rawResults)
            else -> throw IllegalArgumentException("unknown results type to be processed")
        }
    }

    private fun processKOV2(rawResults: KOV2ResultsData): ProcessedResults {
        val candidates = rawResults
            .flatMap { it.votesAndMandates }
            .flatMap { it.candidates.map { cand -> cand.toResult(it.code ?: "ÜKSIK") } }
            .sortedByDescending { it.votes }
        val districts = rawResults.map { it.toResult() }
        return ProcessedResults(districts, candidates, emptyList())
    }

    private fun processRK2(rawResults: RK2Result): ProcessedResults {
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand.toResult(it.code ?: "ÜKSIK") } }
            .sortedByDescending { it.votes }
        val globalDistrict = rawResults.toResult()
        val districts = rawResults.districts.map { it.toResult() }
            .plus(globalDistrict)
        val coalitionPossibilities = try {
            generateCoalitionPossibilities(globalDistrict.parties)
        } catch (e: Exception) {
            emptySet()
        }
        return ProcessedResults(districts, candidates, coalitionPossibilities.toList())
    }

    private fun generateCoalitionPossibilities(parties: List<Party>, currentlySelected: List<String> = emptyList(), currentScore: Int = 0): Set<List<String>> {
        val result = mutableSetOf<List<String>>()
        for ((index, party) in parties.withIndex()) {
            val scoreWithCurrent = currentScore + party.mandates
            val selectedWithCurrent = (currentlySelected + party.code).sorted()
            if (scoreWithCurrent > 50) {
                result.add(selectedWithCurrent)
            } else {
                result.addAll(
                    generateCoalitionPossibilities(
                        parties.subList(index + 1, parties.size),
                        selectedWithCurrent,
                        scoreWithCurrent
                    )
                )
            }
        }
        return result
    }

}