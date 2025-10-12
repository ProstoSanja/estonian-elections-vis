package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.module.kotlin.readValue
import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.mapper
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.restTemplate
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsData
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service

@Service
class RestRepo {
    final inline fun <reified T : ElectionResultsData> fetchElectionData(electionType: ElectionType): T {
        return restTemplate.exchange(
            "https://opendata.valimised.ee/api/${electionType.visCode}/RESULTS.xml",
            HttpMethod.GET,
            null,
            String::class.java
        ).let { mapper.readValue<ElectionResultsRoot<T>>(it.body!!).data }
    }
}