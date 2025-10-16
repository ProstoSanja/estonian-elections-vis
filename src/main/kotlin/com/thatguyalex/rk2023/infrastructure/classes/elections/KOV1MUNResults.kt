package com.thatguyalex.rk2023.infrastructure.classes.elections

data class KOV1MUNResultsData(
    val electionResult: KOV1MUNElectionResult,
) : GOVResultsData

data class KOV1MUNElectionResult(
    val fivePercentage: String,
    val validVotedCount: Int?,
    val participationRows: List<KOV1MUNParticipation> = emptyList(),
    val partyVotesAndMandatesRows: List<KOV1MUNParty> = emptyList(),
    val districts: List<KOV1MUNDistrict> = emptyList(),
)

data class KOV1MUNParticipation(
    val name: String,
    val totalVotersInList: Int,
    val votedCount: Int?,
    val paperVotes: Int?,
    val eVotes: Int?,
    val percentage: String?,
)

data class KOV1MUNParty(
    val sequenceNo: Int?,
    val name: String,
    val votesAndMandatesRows: List<KOV1MUNVotesOrMandates> = emptyList(),
)

data class KOV1MUNDistrict(
    val districtNumber: Int,
    val mandateCount: Int?,
    val simpleQuota: String,
    val voteDistributionByParties: List<KOV1MUNDistrictParty> = emptyList(),
    val votesDistributionRow: List<KOV1MUNVotesOrMandates> = emptyList(),
)

data class KOV1MUNDistrictParty(
    val name: String,
    val candidates: List<KOV1MUNCandidate> = emptyList(),
    val votesDistributionRow: List<KOV1MUNVotesOrMandates> = emptyList(),
)

data class KOV1MUNCandidate(
    val name: String,
    val candidateRegNumber: Int,
    val vrd: String,
    val elected: Boolean,
    val votesDistributionRow: List<KOV1MUNVotesOrMandates> = emptyList(),
)

data class KOV1MUNVotesOrMandates(
    val name: String,
    val value: Double?,
)

fun List<KOV1MUNVotesOrMandates>.getTotalVotes(): Int {
    return find { it.name == "R" }?.value?.toInt() ?: 0
}
fun List<KOV1MUNVotesOrMandates>.getEVotes(): Int {
    return find { it.name == "E" }?.value?.toInt() ?: 0
}