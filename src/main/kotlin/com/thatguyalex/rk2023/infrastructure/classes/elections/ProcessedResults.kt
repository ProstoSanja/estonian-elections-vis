package com.thatguyalex.rk2023.infrastructure.classes.elections

class ProcessedResults(
    val partiesList: List<Party>,
    val districtsList: List<District>,
    val candidatesList: List<Candidate>,
) {
    val parties: Map<String, Party> = partiesList.associateBy { it.code }
    val districts: Map<Int, District> = districtsList.associateBy { it.number }
    val candidates: Map<String, Candidate> = candidatesList.associateBy { it.uniqueId }
}

data class Party(
    val name: String,
    val code: String,
    val mandates: Int,
    val votes: Int,
)

data class District(
    val name: String,
    val number: Int,
    val parties: List<Party>,
    val voteStats: VoteStats,
    val totalMandates: Int,
)

data class VoteStats(
    val votesCounted: Int,
    val protocolsCounted: Int,
    val protocolsTotal: Int,
    val evotesCounted: Boolean,
)

data class Candidate(
    val forename: String,
    val surename: String,
    val regNumber: Int,
    val votes: Int,
    val partyCode: String,
    val districtNumber: Int,
) {
    val uniqueId: String = "$districtNumber-$regNumber"
}