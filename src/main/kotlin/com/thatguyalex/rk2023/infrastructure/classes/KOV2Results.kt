package com.thatguyalex.rk2023.infrastructure.classes

import com.fasterxml.jackson.annotation.JsonProperty

class KOV2ResultsData: ElectionResultsData, ArrayList<KOV2AdminUnitResult>()

data class KOV2AdminUnitResult(
    val adminUnit: KOV2AdminUnitDescription,
    val statistics: ElectionStatistics,
    val mandateCount: Int?,
    val fivePercentage: Double?,
    val votesAndMandates: List<KOV2Party>,
)

data class KOV2AdminUnitDescription(
    val name: String,
    @JsonProperty("code")
    val ehakCode: String,
    val parentAdminUnitName: String?,
    val parentEhakCode: String?,
)

data class KOV2Party(
    val name: String,
    val code: String?,
    val votes: Int,
    val numberOfMandates: Int,
    val percentage: Double,
    @JsonProperty("electedAndReservedCandidates")
    val candidates: List<KOV2Candidate> = emptyList()
)

data class KOV2Candidate(
    val forename: String,
    val surname: String,
    val registrationNumber: Int,
    val positionNumberInAdminUnit: Int?,
    val positionNumberInDistrict: Int?,
    val votes: Int,
    val finalPositionNumber: Int,
    val districtNumber: Int,
    val quota: Float,
    val comparativeFigure: Float,
    val mandateType: ElectionMandateType?,
    val elected: Boolean,
    val reserved: Boolean,
)