package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.module.kotlin.readValue
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.mapper
import com.thatguyalex.rk2023.infrastructure.classes.elections.CAND1ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsRoot
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV2ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.RK2ResultsData
import org.springframework.stereotype.Service

@Service
class ElectionsStorageRepo {

    private val rk2023 = run {
        javaClass.getResourceAsStream("/results/RESULTS_RK2023.xml")!!
            .let { mapper.readValue<GOVResultsRoot<RK2ResultsData>>(it).data }
    }
    private val kov2021 = run {
        javaClass.getResourceAsStream("/results/RESULTS_KOV2021.xml")!!
            .let { mapper.readValue<GOVResultsRoot<KOV2ResultsData>>(it).data }
    }
    private val kov2025Candidates = run {
        javaClass.getResourceAsStream("/results/ELECTION_CANDIDATES_KOV2025.xml")!!
            .let { mapper.readValue<GOVResultsRoot<CAND1ResultsData>>(it).data }
    }

    fun getResults(): MutableMap<ElectionType, GOVResultsData> {
        return mutableMapOf(
            ElectionType.RK2023 to rk2023,
            ElectionType.KOV2021 to kov2021,
        )
    }

    fun getCandidates(): MutableMap<ElectionType, CAND1ResultsData> {
        return mutableMapOf(
            ElectionType.KOV2025 to kov2025Candidates,
        )
    }
}