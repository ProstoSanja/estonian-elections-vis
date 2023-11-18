package com.thatguyalex.rk2023.infrastructure.classes

import java.time.Instant

data class ElectionResultsRoot<T: ElectionResultsData>(
    val electionCode: String,
    val reportType: String,
    val generated: Instant,
    val data: T,
)

interface ElectionResultsData


data class ElectionStatistics(
    val votes: Int,
    val confirmedPollingStationsCount: Int,
    val totalPollingStationsCount: Int,
    val eVotesCounted: Boolean,
)

enum class ElectionMandateType {
    PERSONAL,
    DISTRICT,
    COMPENSATION,
}