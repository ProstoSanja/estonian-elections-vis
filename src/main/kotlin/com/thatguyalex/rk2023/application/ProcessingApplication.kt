package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.District
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.RestRepo
import com.thatguyalex.rk2023.infrastructure.classes.toResult
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProcessingApplication(
    private val restRepo: RestRepo
) {

    private var processedResults = ProcessedResults(emptyList(), emptyList())
    fun getProcessedResults(): ProcessedResults {
        return processedResults
    }
    @Scheduled(fixedRate = 60 * 1000)
    private fun fetchAndProcess() {
        val rawResults= restRepo.fetchElectionData()
        val candidates = rawResults.parties
            .flatMap { it.candidates.map { cand -> cand to it.partyCode } }
            .map { it.first.toResult(it.second) }
            .sortedBy { it.votes }
        val globalDistrict = District(
            name = rawResults.adminUnitName,
            number = 0,
            parties = rawResults.parties.map { it.toResult() }.sortedBy { it.votes },
            voteStats = rawResults.participation.toResult()
        )
        val districts = rawResults.districts
            .map { it.toResult() }
            .plus(globalDistrict)
        processedResults = ProcessedResults(districts, candidates)
    }


}