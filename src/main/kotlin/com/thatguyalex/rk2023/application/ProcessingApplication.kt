package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.District
import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.RestRepo
import com.thatguyalex.rk2023.infrastructure.classes.toResult
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProcessingApplication(
    private val restRepo: RestRepo
) {

    private var processedResults = ProcessedResults(emptyList(), emptyList(), emptyList())
    fun getProcessedResults(): ProcessedResults {
        return processedResults
    }
    @Scheduled(fixedRate = 60 * 1000)
    private fun fetchAndProcess() {
        val rawResults= restRepo.fetchElectionData()
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand to it.partyCode } }
            .map { it.first.toResult(it.second) }
            .sortedByDescending { it.votes }
        val globalParties = rawResults.parties.map { it.toResult() }.sortedBy { it.votes }
        val globalDistrict = District(
            name = rawResults.adminUnitName,
            number = 0,
            parties = globalParties,
            voteStats = rawResults.participation.toResult()
        )
        val districts = rawResults.districts
            .map { it.toResult() }
            .plus(globalDistrict)
        val coalitionPossibilities = try {
            generateCoalitionPossibilities(globalParties)
        } catch (e: Exception) {
            emptySet()
        }
        processedResults = ProcessedResults(districts, candidates, coalitionPossibilities.toList())
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