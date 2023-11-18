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
)

fun RK2District.toResult() = District(
    name = name,
    number = number,
    parties = voteDistribution.map { it.toResult() }.sortedByDescending { it.votes },
    voteStats = statistics.toResult(),
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
    voteStats = statistics.toResult()
)

fun RK2Party.toResult() = Party(
    name = name ?: "Üksikkandidaatid",
    code = code ?: "ÜKSIK",
    mandates = numberOfMandates,
    votes = votes,
)

fun KOV2Candidate.toResult(partyCode: String) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = registrationNumber,
    votes = votes,
    partyCode = partyCode,
)
fun KOV2AdminUnitResult.toResult() = District(
    name = adminUnit.name,
    number = adminUnit.ehakCode.toInt(),
    parties = votesAndMandates.map { it.toResult() }.sortedByDescending { it.votes },
    voteStats = statistics.toResult(),
)

fun KOV2Party.toResult() = Party(
    name = name,
    code = code ?: "ÜKSIK",
    mandates = numberOfMandates,
    votes = votes,
)

fun ElectionStatistics.toResult() = VoteStats(
    votesCounted = votes,
    protocolsCounted = confirmedPollingStationsCount,
    protocolsTotal = totalPollingStationsCount,
    evotesCounted = eVotesCounted,
)