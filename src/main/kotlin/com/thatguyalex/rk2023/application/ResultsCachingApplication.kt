package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.RestRepo
import com.thatguyalex.rk2023.infrastructure.StorageRepo
import com.thatguyalex.rk2023.infrastructure.classes.KOV2ResultsData
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ResultsCachingApplication(
    private val storageRepo: StorageRepo,
    private val restRepo: RestRepo,
    private val processingApplication: ProcessingApplication,
) {
    private val processedCandidates = run {
        storageRepo.getCandidates().mapValues { processingApplication.process(it.value, null) }.toMutableMap()
    }

    private val processedResults = run {
        storageRepo.getResults().mapValues { processingApplication.process(it.value, processedCandidates[it.key]) }.toMutableMap()
    }

    fun getProcessedResults(electionType: ElectionType): ProcessedResults {
        return processedResults[electionType]!!
    }

    @Scheduled(fixedRate = 60 * 1000)
    fun fetchActiveElection() {
        processedResults[ElectionType.KOV2025] = restRepo.fetchElectionData<KOV2ResultsData>(ElectionType.KOV2025)
            .let { processingApplication.process(it, processedCandidates[ElectionType.KOV2025]) }
    }
}