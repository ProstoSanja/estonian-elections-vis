package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.module.kotlin.readValue
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.mapper
import com.thatguyalex.rk2023.infrastructure.classes.elections.CAND1ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionFile
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsRoot
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV1MUNResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.KOV2ResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.RK2ResultsData
import org.springframework.stereotype.Service

@Service
class ElectionsStorageRepo {
    fun loadFile(electionType: ElectionType, fileType: ElectionFile, extraArgs: String = ""): GOVResultsData {
        return when (fileType) {
            ElectionFile.RESULTS -> {
                when (electionType) {
                    ElectionType.KOV2021 -> javaClass.getResourceAsStream("/results/RESULTS_KOV2021.xml")!!
                        .let { mapper.readValue<GOVResultsRoot<KOV2ResultsData>>(it).data }
                    ElectionType.RK2023 -> javaClass.getResourceAsStream("/results/RESULTS_RK2023.xml")!!
                        .let { mapper.readValue<GOVResultsRoot<RK2ResultsData>>(it).data }
                    ElectionType.KOV2025 -> throw NotImplementedError()
                }
            }
            ElectionFile.CANDIDATE -> {
                when (electionType) {
                    ElectionType.KOV2021 -> javaClass.getResourceAsStream("/results/ELECTION_CANDIDATES_KOV2021.xml")
                    ElectionType.RK2023 -> javaClass.getResourceAsStream("/results/ELECTION_CANDIDATES_RK2023.xml")
                    ElectionType.KOV2025 -> javaClass.getResourceAsStream("/results/ELECTION_CANDIDATES_KOV2025.xml")
                }!!.let { mapper.readValue<GOVResultsRoot<CAND1ResultsData>>(it).data }
            }
            ElectionFile.DETAILED_RESULT_PARISH -> {
                when (electionType) {
                    ElectionType.KOV2021 ->  javaClass.getResourceAsStream("/results/DETAILED_RESULT_PARISH_${extraArgs.padStart(4, '0')}_KOV2021.xml")
                    ElectionType.RK2023 -> throw NotImplementedError()
                    ElectionType.KOV2025 -> throw NotImplementedError()
                }!!.let { mapper.readValue<GOVResultsRoot<KOV1MUNResultsData>>(it).data }
            }
        }
    }
}