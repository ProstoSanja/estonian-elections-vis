package com.thatguyalex.rk2023.infrastructure.classes

import com.fasterxml.jackson.annotation.JsonProperty

data class RK1ResultsData(
    @JsonProperty("electionResult")
    val electionResult: RK1Result,
) : ElectionResultsData

data class RK1Result(
    val adminUnitName: String,
    val ehakCode: String,
    val participation: RK1Participation,
    val parties: List<RK1Party>,
    //partyVotesAndMandatesRows
    val districts: List<RK1District>,
)

data class RK1Participation(
    val votes: Int,
    val numberOfProtocols: Int,
    val totalNumberOfProtocols: Int,
    val evotesCounted: Boolean,
)

data class RK1Party(
    val partyName: String,
    val partyCode: String,
    val partyTossUpNumber: Int,
    val partyNumberOfMandates: Int,
    val candidates: List<RK1PartyCandidate> = emptyList(),
    val partyVotes: Int,
    val percentage: String?,
)

data class RK1PartyCandidate(
    val forename: String,
    val surname: String,
    val candidateRegNumber: Int,
    val candidateId: Int,
    val candidateVotes: Int,
    val partyName: String,
    val districtNumber: Int,
    val quota: Float,
    val vrd: Float,
    val v1: Boolean,
    val v2: Boolean,
    val v3: Boolean,
    val elected: Boolean,
    val reserved: Boolean,
)

data class RK1District(
    val districtName: String,
    val districtNumber: Int,
    val voteDistributionByParties: List<RK1Party>,
    val districtVotes: RK1DistrictVotes
)

data class RK1DistrictVotes(
    val districtNumberOfVotes: Int,
    val districtNumberOfProtocols: Int,
    val districtTotalNumberOfProtocols: Int,
    val evotesCounted: Boolean,
)