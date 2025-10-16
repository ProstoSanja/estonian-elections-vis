package com.thatguyalex.rk2023.infrastructure.classes.elections

fun RK2PartyCandidate.toResult(partyCode: String) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = registrationNumber,
    votes = votes,
    partyCode = partyCode,
    primaryDistrictNumber = districtNumber,
    districtNumbers = listOf(districtNumber),
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

fun KOV2Candidate.toResult(partyCode: String, primaryDistrictNumber: Int, districtNumbers: List<Int>) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = registrationNumber,
    votes = votes,
    partyCode = partyCode,
    primaryDistrictNumber = primaryDistrictNumber,
    districtNumbers = districtNumbers
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

fun GOVElectionStatistics.toResult() = VoteStats(
    votesCounted = votes,
    protocolsCounted = confirmedPollingStationsCount,
    protocolsTotal = totalPollingStationsCount,
    evotesCounted = eVotesCounted,
)

fun CAND1Candidate.toResult(partyCode: String, primaryDistrictNumber: Int, districtNumbers: List<Int>) = Candidate(
    forename = forename,
    surename = surname,
    regNumber = candidateRegNumber,
    votes = 0,
    partyCode = partyCode,
    primaryDistrictNumber = primaryDistrictNumber,
    districtNumbers = districtNumbers,
)

fun List<CAND1Party>.cand1ListtoResult() = Party(
    name = first().partyName,
    code = first().partyCode,
    mandates = 0,
    votes = 0,
)

fun Int.subDistrictCodeFor(subDistrict: Int): Int {
    return (this * 10) + subDistrict
}