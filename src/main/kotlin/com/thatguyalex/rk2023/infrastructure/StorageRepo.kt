package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.thatguyalex.rk2023.infrastructure.classes.RK1Result
import com.thatguyalex.rk2023.infrastructure.classes.RK1ResultsRoot
import org.springframework.stereotype.Service


@Service
class StorageRepo {

    private val rk2023 = run {
        val file = javaClass.getResourceAsStream("/results/RESULTS_RK2023.xml")!!
        XmlMapper().registerModule(JavaTimeModule()).registerKotlinModule()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .readValue<RK1ResultsRoot>(file).data.electionResult
    }

    //https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
    //http://localhost:12345/EXAMPLE_RESULTS.xml
    fun getResults(): RK1Result {
        return rk2023
    }

}