package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.Candidate
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
        val districts = rawResults.districts
            .map { it.toResult() }
        // add region 0
        processedResults = ProcessedResults(districts, candidates)
    }


}