package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.ElectionsResultsCachingApplication
import com.thatguyalex.rk2023.infrastructure.classes.elections.ElectionType
import com.thatguyalex.rk2023.infrastructure.classes.elections.ProcessedResults
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api", produces=["application/json"])
class ElectionDataController(
    private val electionsResultsCachingApplication: ElectionsResultsCachingApplication
) {

    @GetMapping("/data/{electionType}")
    fun getData(@PathVariable("electionType") electionType: ElectionType): ProcessedResults {
        return electionsResultsCachingApplication.getProcessedResults(electionType)
    }

}