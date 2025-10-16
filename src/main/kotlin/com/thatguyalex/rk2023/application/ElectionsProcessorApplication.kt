package com.thatguyalex.rk2023.application

import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import org.springframework.stereotype.Service

@Service
class ElectionsProcessorApplication(
    private val electionsDataConversionApplication: ElectionsDataConversionApplication,
) {

    val candidatesCache = mutableMapOf<ElectionType, ProcessedResults>()

    fun processElection(electionType: ElectionType, results: GOVResultsData, candidates: () -> GOVResultsData, detailedMunicipalities: Map<String, GOVResultsData>): ProcessedResults {
        val coreCandidates = candidatesCache.getOrPut(electionType) { electionsDataConversionApplication.process(candidates(), null) }
        val coreResults = electionsDataConversionApplication.process(results, coreCandidates)
            .addExcluded(coreCandidates)
        val enrichedResult = detailedMunicipalities.map { electionsDataConversionApplication.process(it.value, coreCandidates, it.key) }
            .fold(coreResults) { acc, details -> acc.addOverriding(details) }
        return enrichedResult.sorted()
    }
}