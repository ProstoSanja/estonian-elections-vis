package com.thatguyalex.rk2023.infrastructure.classes.elections

import com.thatguyalex.rk2023.application.ElectionsDataConversionApplication.Companion.candidateSort
import com.thatguyalex.rk2023.application.ElectionsDataConversionApplication.Companion.districtSort
import com.thatguyalex.rk2023.application.ElectionsDataConversionApplication.Companion.partySort

class ProcessedResults(
    val partiesList: List<Party>,
    val districtsList: List<District>,
    val candidatesList: List<Candidate>,
) {
    val parties: Map<String, Party> = partiesList.associateBy { it.code }
    val districts: Map<Int, District> = districtsList.associateBy { it.number }
    val candidates: Map<String, Candidate> = candidatesList.associateBy { it.uniqueId }

    fun addExcluded(newResults: ProcessedResults): ProcessedResults {
        val partiesToAdd = (newResults.parties.keys - parties.keys).map { newResults.parties[it]!! }
        val districtsToAdd = (newResults.districts.keys - districts.keys).map { newResults.districts[it]!! }
        val candidatesToAdd = (newResults.candidates.keys - candidates.keys).map { newResults.candidates[it]!! }

        return ProcessedResults(partiesList + partiesToAdd, districtsList + districtsToAdd, candidatesList + candidatesToAdd)
    }

    fun addOverriding(newResults: ProcessedResults): ProcessedResults {
        val parties = parties.plus(newResults.parties).values.toList()
        val districts = districts.plus(newResults.districts).values.toList()
        val candidates = candidates.plus(newResults.candidates).values.toList()
        return ProcessedResults(parties, districts, candidates)
    }

    fun sorted(): ProcessedResults {
        return ProcessedResults(partiesList.sortedWith(partySort), districtsList.sortedWith(districtSort), candidatesList.sortedWith(candidateSort))
    }

    companion object {
        fun empty(): ProcessedResults = ProcessedResults(emptyList(), emptyList(), emptyList())
    }
}

data class Party(
    val name: String,
    val code: String,
    val mandates: Int,
    val votes: Int,
)

data class District(
    val name: String,
    val number: Int,
    val parties: List<Party>,
    val voteStats: VoteStats,
    val totalMandates: Int,
)

data class VoteStats(
    val votesCounted: Int,
    val protocolsCounted: Int,
    val protocolsTotal: Int,
    val evotesCounted: Boolean,
) {
    companion object {
        fun empty() = VoteStats(
            votesCounted = 0,
            protocolsCounted = 0,
            protocolsTotal = 0,
            evotesCounted = false,
        )
    }
}

data class Candidate(
    val forename: String,
    val surename: String,
    val regNumber: Int,
    val votes: Int,
    val partyCode: String,
    val districtNumber: Int,
) {
    val uniqueId: String = "$districtNumber-$regNumber"
}