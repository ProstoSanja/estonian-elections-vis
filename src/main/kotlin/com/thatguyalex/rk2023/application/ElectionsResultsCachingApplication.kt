package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.ElectionsRestRepo
import com.thatguyalex.rk2023.infrastructure.ElectionsStorageRepo
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionFile
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionsDataUpdatedEvent
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV1MUNResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV2ResultsData
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ElectionsResultsCachingApplication(
    private val electionsStorageRepo: ElectionsStorageRepo,
    private val electionsRestRepo: ElectionsRestRepo,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val electionsProcessorApplication: ElectionsProcessorApplication
) {
    private val processedResults = mutableMapOf<ElectionType, ProcessedResults>()

    fun getProcessedResults(electionType: ElectionType): ProcessedResults {
        return processedResults[electionType] ?: ProcessedResults.empty()
    }

//    @Scheduled(fixedRate = 60 * 1000)
    fun fetchActiveElection() {
        val activeElection = ElectionType.KOV2025
        val newCoreResults = electionsRestRepo.fetchElectionData<KOV2ResultsData>(activeElection, ElectionFile.RESULTS)
        val newTallinnResults = try {
            mapOf("784" to electionsRestRepo.fetchElectionData<KOV1MUNResultsData>(activeElection, ElectionFile.DETAILED_RESULT_PARISH, "784"))
        } catch (e: Exception) {
            emptyMap()
        }
        val electionResults = electionsProcessorApplication.processElection(activeElection, newCoreResults, { electionsStorageRepo.loadFile(activeElection, ElectionFile.CANDIDATE)}, newTallinnResults)
        processedResults[activeElection]
            ?.let { applicationEventPublisher.publishEvent(ElectionsDataUpdatedEvent(this, activeElection, it, electionResults)) }
        processedResults[activeElection] = electionResults
    }

    @EventListener(ApplicationReadyEvent::class)
    fun loadPastElections() {
        processedResults[ElectionType.KOV2021] = electionsProcessorApplication.processElection(
            electionType = ElectionType.KOV2021,
            results = electionsStorageRepo.loadFile(ElectionType.KOV2021, ElectionFile.RESULTS),
            candidates = { electionsStorageRepo.loadFile(ElectionType.KOV2021, ElectionFile.CANDIDATE) },
            detailedMunicipalities = mapOf("784" to electionsStorageRepo.loadFile(ElectionType.KOV2021, ElectionFile.DETAILED_RESULT_PARISH, "784"))
        )
        processedResults[ElectionType.RK2023] = electionsProcessorApplication.processElection(
            electionType = ElectionType.RK2023,
            results = electionsStorageRepo.loadFile(ElectionType.RK2023, ElectionFile.RESULTS),
            candidates = { electionsStorageRepo.loadFile(ElectionType.RK2023, ElectionFile.CANDIDATE) },
            detailedMunicipalities = emptyMap()
        )
        processedResults[ElectionType.KOV2025] = electionsProcessorApplication.processElection(
            electionType = ElectionType.KOV2025,
            results = electionsStorageRepo.loadFile(ElectionType.KOV2025, ElectionFile.RESULTS),
            candidates = { electionsStorageRepo.loadFile(ElectionType.KOV2025, ElectionFile.CANDIDATE) },
            detailedMunicipalities = mapOf("784" to electionsStorageRepo.loadFile(ElectionType.KOV2025, ElectionFile.DETAILED_RESULT_PARISH, "784"))
        )
    }
}