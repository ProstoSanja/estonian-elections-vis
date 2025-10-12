package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.module.kotlin.readValue
import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.mapper
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsData
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import com.thatguyalex.rk2023.infrastructure.classes.KOV2ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.RK2ResultsData
import org.springframework.stereotype.Service

@Service
class StorageRepo {

    private val rk2023 = run {
        javaClass.getResourceAsStream("/results/RESULTS_RK2023.xml")!!
            .let { mapper.readValue<ElectionResultsRoot<RK2ResultsData>>(it).data }
    }
    private val kov2021 = run {
        javaClass.getResourceAsStream("/results/RESULTS_KOV2021.xml")!!
            .let { mapper.readValue<ElectionResultsRoot<KOV2ResultsData>>(it).data }
    }

    fun getResults(): MutableMap<ElectionType, ElectionResultsData> {
        return mutableMapOf(
            ElectionType.RK2023 to rk2023,
            ElectionType.KOV2021 to kov2021,
        )
    }
}