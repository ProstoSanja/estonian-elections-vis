package com.thatguyalex.rk2023.infrastructure.classes

import com.thatguyalex.rk2023.application.classes.Candidate
import com.thatguyalex.rk2023.application.classes.District
import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.VoteStats

fun RK2PartyCandidate.toResult(partyCode: String) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = registrationNumber,
    votes = votes,
    partyCode = partyCode,
    districtNumber = districtNumber,
)

fun RK2District.toResult(allCandidates: List<RK2PartyCandidate>) = District(
    name = name,
    number = number,
    parties = voteDistribution.map { it.toResult() }.sortedByDescending { it.votes },
    voteStats = statistics.toResult(),
    totalMandates = allCandidates.count { it.districtNumber == number && it.mandateType != null }
)

fun RK2DistrictVotes.toResult() = Party(
    name = name,
    code = code ?: "ÜKSIK",
    mandates = 0,
    votes = votes,
)

fun RK2Result.toResult() = District(
    name = adminUnitName,
    number = ehakCode.toInt(),
    parties = parties.map { it.toResult() }.sortedByDescending { it.votes },
    voteStats = statistics.toResult(),
    totalMandates = parties.sumOf { it.numberOfMandates }
)

fun RK2Party.toResult() = Party(
    name = name ?: "Üksikkandidaadid",
    code = code ?: "ÜKSIK",
    mandates = numberOfMandates,
    votes = votes,
)

fun KOV2Candidate.toResult(partyCode: String, districtNumber: Int) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = registrationNumber,
    votes = votes,
    partyCode = partyCode,
    districtNumber = districtNumber,
)

fun KOV2AdminUnitResult.toResult(allDistricts: List<KOV2AdminUnitResult>): District {
    val subDistricts = if (adminUnit.ehakCode.toInt() == 0) allDistricts else allDistricts.filter { sub -> sub.adminUnit.parentEhakCode == adminUnit.ehakCode }
    val subParties = subDistricts.flatMap { it.votesAndMandates }.groupBy { party -> (party.code ?: "ÜKSIK").takeIf { !party.name.lowercase().contains("liit") } ?: "VAL_LIIDUD" }
    return District(
        name = adminUnit.name.substringBefore("(").trim(),
        number = adminUnit.ehakCode.toInt(),
        parties = votesAndMandates.map { it.toResult(subParties[it.code ?: "ÜKSIK"]?.sumOf { it.numberOfMandates ?: 0 } ?: 0) }.sortedByDescending { it.votes },
        voteStats = statistics.toResult(),
        totalMandates = mandateCount ?: subDistricts.sumOf { it.mandateCount ?: 0 }
    )
}

fun KOV2Party.toResult(mandatesOverride: Int = 0) = Party(
    name = name,
    code = code ?: "ÜKSIK",
    mandates = numberOfMandates ?: mandatesOverride,
    votes = votes,
)

fun List<KOV2Party>.kov2ListToResult() = Party(
    name = first().name,
    code = first().code ?: "ÜKSIK",
    mandates = sumOf { it.numberOfMandates ?: 0 },
    votes = sumOf { it.votes }
)

fun ElectionStatistics.toResult() = VoteStats(
    votesCounted = votes,
    protocolsCounted = confirmedPollingStationsCount,
    protocolsTotal = totalPollingStationsCount,
    evotesCounted = eVotesCounted,
)

fun CAND1Candidate.toResult(partyCode: String, districtNumber: Int) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = candidateRegNumber,
    votes = 0,
    partyCode = partyCode,
    districtNumber = districtNumber,
)

fun List<CAND1Party>.cand1ListtoResult() = Party(
    name = first().partyName,
    code = first().partyCode,
    mandates = 0,
    votes = 0,
)