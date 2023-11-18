package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResult
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import org.springframework.stereotype.Service
import java.io.File


@Service
class StorageRepo {

    private val rk2023 = run {
        val file = File(javaClass.getResource("/results/RESULTS_2023.xml")!!.file)
        XmlMapper().registerModule(JavaTimeModule()).registerKotlinModule()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .readValue<ElectionResultsRoot>(file).data.electionResult
    }

    //https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
    //http://localhost:12345/EXAMPLE_RESULTS.xml
    fun getResults(): ElectionResult {
        return rk2023
    }

}