package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.ElectionsRestRepo
import com.thatguyalex.rk2023.infrastructure.ElectionsStorageRepo
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionsDataUpdatedEvent
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV2ResultsData
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ElectionsResultsCachingApplication(
    private val electionsStorageRepo: ElectionsStorageRepo,
    private val electionsRestRepo: ElectionsRestRepo,
    private val electionsDataProcessingApplication: ElectionsDataProcessingApplication,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    private val processedCandidates = run {
        electionsStorageRepo.getCandidates().mapValues { electionsDataProcessingApplication.process(it.value, null) }.toMutableMap()
    }

    private val processedResults = run {
        electionsStorageRepo.getResults().mapValues { electionsDataProcessingApplication.process(it.value, processedCandidates[it.key]) }.toMutableMap()
    }

    fun getProcessedResults(electionType: ElectionType): ProcessedResults {
        return processedResults[electionType]!!
    }

    @Scheduled(fixedRate = 60 * 1000)
//    @Scheduled(fixedRate = 15 * 1000) // mock
    fun fetchActiveElection() {
//    val newResults = electionsRestRepo.fetchMockElectionData<KOV2ResultsData>(ElectionType.KOV2025)
        val newResults = electionsRestRepo.fetchElectionData<KOV2ResultsData>(ElectionType.KOV2025)
            .let { electionsDataProcessingApplication.process(it, processedCandidates[ElectionType.KOV2025]) }
        processedResults[ElectionType.KOV2025]
            ?.let { applicationEventPublisher.publishEvent(ElectionsDataUpdatedEvent(this, ElectionType.KOV2025, it, newResults)) }
        processedResults[ElectionType.KOV2025] = newResults
    }
}