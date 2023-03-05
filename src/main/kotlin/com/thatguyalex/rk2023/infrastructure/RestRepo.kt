package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.infrastructure.classes.ElectionResult
import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RestRepo {

    private val restTemplate = RestTemplateBuilder()
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(10))
        .build()

    //https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
    //http://localhost:12345/EXAMPLE_RESULTS.xml
    fun fetchElectionData(): ElectionResult {
        val results = restTemplate.getForEntity("https://opendata.valimised.ee/api/RK_2023/RESULTS.xml", ElectionResultsRoot::class.java)
        return results.body!!.data.electionResult
    }

}