package com.thatguyalex.rk2023.application.classes

data class ProcessedResults(
    val districts: List<District>,
    val candidates: List<Candidate>,
    val coalitionPossibilities: List<List<String>>,
)

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
    val voteStats: VoteStats
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
)