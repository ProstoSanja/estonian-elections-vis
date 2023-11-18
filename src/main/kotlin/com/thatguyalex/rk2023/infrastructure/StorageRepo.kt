package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.thatguyalex.rk2023.application.ProcessingApplication
import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import com.thatguyalex.rk2023.infrastructure.classes.RK1ResultsData
import org.springframework.stereotype.Service


@Service
class StorageRepo(
    private val processingApplication: ProcessingApplication,
) {
    private val mapper = XmlMapper()
        .registerModule(JavaTimeModule()).registerKotlinModule()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val rk2023 = run {
        javaClass.getResourceAsStream("/results/RESULTS_RK2023.xml")!!
            .let { mapper.readValue<ElectionResultsRoot<RK1ResultsData>>(it).data.electionResult }
            .let { processingApplication.fetchAndProcess(it) }
    }

    //https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
    //http://localhost:12345/EXAMPLE_RESULTS.xml
    fun getResults(): MutableMap<ElectionType, ProcessedResults> {
        return mutableMapOf(ElectionType.RK2023 to rk2023)
    }

}