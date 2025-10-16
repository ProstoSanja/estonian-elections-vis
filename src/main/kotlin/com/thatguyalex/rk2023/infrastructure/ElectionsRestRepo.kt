package com.thatguyalex.rk2023.infrastructure

import com.fasterxml.jackson.module.kotlin.readValue
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.mapper
import com.thatguyalex.rk2023.infrastructure.ElectionResultsParser.Companion.restTemplate
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsData
import com.thatguyalex.rk2023.infrastructure.classes.elections.GOVResultsRoot
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service

@Service
class ElectionsRestRepo {
    final inline fun <reified T : GOVResultsData> fetchElectionData(electionType: ElectionType): T {
        return restTemplate.exchange(
            "https://opendata.valimised.ee/api/${electionType.visCode}/RESULTS.xml",
            HttpMethod.GET,
            null,
            String::class.java
        ).let { mapper.readValue<GOVResultsRoot<T>>(it.body!!).data }
    }
}