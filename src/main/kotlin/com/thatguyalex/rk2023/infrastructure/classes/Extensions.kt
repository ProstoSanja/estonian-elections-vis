package com.thatguyalex.rk2023.infrastructure.classes

import com.thatguyalex.rk2023.application.classes.Candidate
import com.thatguyalex.rk2023.application.classes.District
import com.thatguyalex.rk2023.application.classes.Party
import com.thatguyalex.rk2023.application.classes.VoteStats

fun ElectionPartyCandidate.toResult(partyCode: String) = Candidate(
    forename = forename,
    surname = surname,
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

fun ElectionDistrict.toResult() = District(
    name = districtName,
    number = districtNumber,
    parties = voteDistributionByParties.map { it.toResult() },
    voteStats = districtVotes.toResult(),

)

fun ElectionDistrictVotes.toResult() = VoteStats(
    votesCounted = districtNumberOfVotes,
    protocolsCounted = districtNumberOfProtocols,
    protocolsTotal = districtTotalNumberOfProtocols,
    evotesCounted = evotesCounted,
)

fun ElectionParticipation.toResult() = VoteStats(
    votesCounted = votes,
    protocolsCounted = numberOfProtocols,
    protocolsTotal = totalNumberOfProtocols,
    evotesCounted = evotesCounted,
)

fun ElectionParty.toResult() = Party(
    name = partyName,
    code = partyCode,
    mandates = partyNumberOfMandates,
    votes = partyVotes,
)