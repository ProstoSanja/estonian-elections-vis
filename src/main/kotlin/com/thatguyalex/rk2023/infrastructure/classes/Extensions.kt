package com.thatguyalex.rk2023.infrastructure.classes

import com.thatguyalex.rk2023.application.classes.Candidate
import com.thatguyalex.rk2023.application.classes.District
import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.VoteStats

fun RK1PartyCandidate.toResult(partyCode: String) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = candidateRegNumber,
    votes = candidateVotes,
    partyCode = partyCode,
    districtNumber = districtNumber,
    quota = quota,
    vrd = vrd,
    v1 = v1,
    v2 = v2,
    v3 = v3,
    elected = elected,
    reserved = reserved
)

fun RK1District.toResult() = District(
    name = districtName,
    number = districtNumber,
    parties = voteDistributionByParties.map { it.toResult() }.sortedByDescending { it.votes },
    voteStats = districtVotes.toResult(),

)

fun RK1DistrictVotes.toResult() = VoteStats(
    votesCounted = districtNumberOfVotes,
    protocolsCounted = districtNumberOfProtocols,
    protocolsTotal = districtTotalNumberOfProtocols,
    evotesCounted = evotesCounted,
)

fun RK1Participation.toResult() = VoteStats(
    votesCounted = votes,
    protocolsCounted = numberOfProtocols,
    protocolsTotal = totalNumberOfProtocols,
    evotesCounted = evotesCounted,
)

fun RK1Party.toResult() = Party(
    name = partyName,
    code = partyCode,
    mandates = partyNumberOfMandates,
    votes = partyVotes,
)