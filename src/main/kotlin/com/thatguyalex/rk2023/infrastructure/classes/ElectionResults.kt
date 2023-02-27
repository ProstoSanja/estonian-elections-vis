package com.thatguyalex.rk2023.infrastructure.classes

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class ElectionResultsRoot(
    val electionCode: String,
    val reportType: String,
    val generated: Instant,
    val data: ElectionResultsData,
)

data class ElectionResultsData(
    @JsonProperty("electionResult")
    val electionResult: ElectionResult,
)

data class ElectionResult(
    val adminUnitName: String,
    val ehakCode: String,
    val participation: ElectionParticipation,
    val parties: List<ElectionParty>,
    //partyVotesAndMandatesRows
    val districts: List<ElectionDistrict>,
)

data class ElectionParticipation(
    val votes: Int,
    val numberOfProtocols: Int,
    val totalNumberOfProtocols: Int,
    val evotesCounted: Boolean,
)

data class ElectionParty(
    val partyName: String,
    val partyCode: String,
    val partyTossUpNumber: Int,
    val partyNumberOfMandates: Int,
    val candidates: List<ElectionPartyCandidate>,
    val partyVotes: Int,
    val percentage: String,
)

data class ElectionPartyCandidate(
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

data class ElectionDistrict(
    val districtName: String,
    val districtNumber: Int,
    val voteDistributionByParties: List<ElectionParty>,
    val districtVotes: ElectionDistrictVotes
)

data class ElectionDistrictVotes(
    val districtNumberOfVotes: Int,
    val districtNumberOfProtocols: Int,
    val districtTotalNumberOfProtocols: Int,
    val evotesCounted: Boolean,
)