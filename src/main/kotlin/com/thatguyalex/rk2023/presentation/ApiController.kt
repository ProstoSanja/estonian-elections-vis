package com.thatguyalex.rk2023.presentation

import com.thatguyalex.rk2023.application.ProcessingApplication
import com.thatguyalex.rk2023.application.classes.ProcessedResults
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api", produces=["application/json"])
class ApiController(
    private val processingApplication: ProcessingApplication
) {

    @GetMapping("/data")
    fun getData(): ProcessedResults {
        return processingApplication.getProcessedResults()
    }

}