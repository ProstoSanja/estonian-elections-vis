package com.thatguyalex.rk2023.infrastructure.classes.elections

import java.time.Instant

data class GOVResultsRoot<T: GOVResultsData>(
    val electionCode: String,
    val reportType: String,
    val generated: Instant,
    val data: T,
)

interface GOVResultsData


data class GOVElectionStatistics(
    val votes: Int,
    val confirmedPollingStationsCount: Int,
    val totalPollingStationsCount: Int,
    val eVotesCounted: Boolean,
)

enum class GOVCandidateMandateType {
    PERSONAL,
    DISTRICT,
    COMPENSATION,
}
