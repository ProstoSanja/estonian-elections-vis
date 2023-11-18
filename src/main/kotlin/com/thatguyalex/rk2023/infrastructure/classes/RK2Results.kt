package com.thatguyalex.rk2023.infrastructure.classes

import com.fasterxml.jackson.annotation.JsonProperty

data class RK2ResultsData(
    @JsonProperty("electionResult")
    val electionResult: RK2Result,
) : ElectionResultsData

data class RK2Result(
    val adminUnitName: String,
    val ehakCode: String,
    val statistics: ElectionStatistics,
    @JsonProperty("votesAndMandates")
    val parties: List<RK2Party>,
    val districts: List<RK2District>,
)

data class RK2Party(
    val name: String?,
    val code: String?,
    val votes: Int,
    val percentage: Double,
    val numberOfMandates: Int,
    val positionLotteryNr: Int?,
    val candidates: List<RK2PartyCandidate> = emptyList(),
)

data class RK2PartyCandidate(
    val forename: String,
    val surname: String,
    val registrationNumber: Int,
    val applicationId: Int,
    val votes: Int,
    val comparativeFigure: Double,
    val elected: Boolean,
    val reserved: Boolean,
    val finalPositionNumber: Int?,
    val quota: Float,
    val districtNumber: Int,
    val mandateType: ElectionMandateType?,
)

data class RK2District(
    val name: String,
    val number: Int,
    val voteDistribution: List<RK2DistrictVotes>,
    val statistics: ElectionStatistics
)

data class RK2DistrictVotes(
    val name: String,
    val code: String?,
    val votes: Int,
    val percentage: Double,
)