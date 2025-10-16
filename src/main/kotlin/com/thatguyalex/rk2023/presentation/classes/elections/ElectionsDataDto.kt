package com.thatguyalex.rk2023.presentation.classes.elections

import com.thatguyalex.rk2023.infrastructure.classes.elections.Candidate
import com.thatguyalex.rk2023.infrastructure.classes.elections.Party
import com.thatguyalex.rk2023.infrastructure.classes.elections.District
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults

data class ElectionsDataDto(
    val parties: List<Party>,
    val districts: List<District>,
    val candidates: List<Candidate>,
) {
    companion object {
        fun from(processedResults: ProcessedResults) = ElectionsDataDto(
            parties = processedResults.partiesList,
            districts = processedResults.districtsList,
            candidates = processedResults.candidatesList,
        )
    }   
}
