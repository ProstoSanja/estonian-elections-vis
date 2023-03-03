package com.thatguyalex.rk2023.infrastructure

import com.thatguyalex.rk2023.infrastructure.classes.ElectionResultsRoot
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RestRepo {

    private val restTemplate = RestTemplateBuilder()
        .setConnectTimeout(Duration.ofSeconds(10))
        .setReadTimeout(Duration.ofSeconds(10))
        .build()

    //https://opendata.valimised.ee/api/RK_2023/RESULTS.xml
    @Scheduled(fixedRate = 60 * 1000)
    fun fetchElectionData() {
        val results = restTemplate.getForEntity("http://localhost:8080/EXAMPLE_RESULTS.xml", ElectionResultsRoot::class.java)
        val usefulResults = results.body!!.data.electionResult
    }

}