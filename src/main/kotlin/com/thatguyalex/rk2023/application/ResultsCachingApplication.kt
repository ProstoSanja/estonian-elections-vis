package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.StorageRepo
import org.springframework.stereotype.Service

@Service
class ResultsCachingApplication(
    private val storageRepo: StorageRepo,
    private val processingApplication: ProcessingApplication,
) {
    private val processedResults = run {
        storageRepo.getResults().mapValues { processingApplication.process(it.value) }
    }

    fun getProcessedResults(electionType: ElectionType): ProcessedResults {
        return processedResults[electionType]!!
    }
}