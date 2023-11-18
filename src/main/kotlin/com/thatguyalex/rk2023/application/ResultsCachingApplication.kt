package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.StorageRepo
import org.springframework.stereotype.Service

@Service
class ResultsCachingApplication(
    private val processingApplication: ProcessingApplication,
    private val storageRepo: StorageRepo,
) {
    private val processedResults = mutableMapOf<ElectionType, ProcessedResults>()

    init {
        val rk2023 = storageRepo.getResults()
        val processedRk2023 = processingApplication.fetchAndProcess(rk2023)
        processedResults[ElectionType.RK2023] = processedRk2023
    }
    fun getProcessedResults(electionType: ElectionType): ProcessedResults {
        return processedResults[electionType]!!
    }
}