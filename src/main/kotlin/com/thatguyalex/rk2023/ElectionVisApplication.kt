package com.thatguyalex.rk2023

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.thatguyalex"])
class ElectionVisApplication

fun main(args: Array<String>) {
    runApplication<ElectionVisApplication>(*args)
}
