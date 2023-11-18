package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.ResultsCachingApplication
import com.thatguyalex.rk2023.application.classes.ElectionType
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api", produces=["application/json"])
class ApiController(
    private val resultsCachingApplication: ResultsCachingApplication
) {

    @GetMapping("/data/{electionType}")
    fun getData(@PathVariable("electionType") electionType: ElectionType): ProcessedResults {
        return resultsCachingApplication.getProcessedResults(electionType)
    }

}