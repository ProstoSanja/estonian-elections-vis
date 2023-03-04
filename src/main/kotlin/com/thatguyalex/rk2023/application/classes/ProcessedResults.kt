package com.thatguyalex.rk2023.application.classes

data class ProcessedResults(
    val districts: List<District>,
    val candidates: List<Candidate>
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
    val surname: String,
    val regNumber: Int,
    val votes: Int,
    val partyCode: String,
    val districtNumber: Int,
    val quota: Float,
    val vrd: Float,
    val v1: Boolean,
    val v2: Boolean,
    val v3: Boolean,
    val elected: Boolean,
    val reserved: Boolean,
)