package com.thatguyalex.rk2023.infrastructure.classes.elections

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

/**
 * Main data container
 * Maps to dataType in election-candidates_v1.xsd
 * Contains either electionDistricts (for EP/RK) OR adminUnits (for KOV)
 */
data class CAND1ResultsData(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "electionDistrict")
    val electionDistricts: List<CAND1ElectionDistrict> = emptyList(),
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "adminUnit")
    val adminUnits: List<CAND1AdminUnit> = emptyList(),
    val numberOfCandidates: Int,
): GOVResultsData

/**
 * Election district structure for EP/RK elections
 * Maps to electionDistrictType in election-candidates_v1.xsd
 */
data class CAND1ElectionDistrict(
    val districtName: String,
    val districtNumber: Int,
    val parties: List<CAND1Party> = emptyList(),
    val numberOfCandidates: Int,
)

/**
 * Admin unit structure for KOV elections
 * Maps to adminUnitType in election-candidates_v1.xsd
 */
data class CAND1AdminUnit(
    val name: String,
    @JsonProperty("code")
    val ehakCode: String,
    val districts: List<CAND1KovDistrict> = emptyList(),
    val childAdminUnits: List<CAND1AdminUnit> = emptyList(),
    val numberOfCandidates: Int,
)

/**
 * KOV district structure
 * Maps to kovDistrictType in election-candidates_v1.xsd
 */
data class CAND1KovDistrict(
    val districtComment: String,
    val districtNumber: Int,
    val parties: List<CAND1Party> = emptyList(),
    val numberOfCandidates: Int,
)

/**
 * Party/political association with candidates
 * Maps to partyType in election-candidates_v1.xsd
 */
data class CAND1Party(
    val partyCode: String,
    val partyName: String,
    val numberOfCandidates: Int,
    val candidates: List<CAND1Candidate> = emptyList(),
)

/**
 * Individual candidate information
 * Maps to candidateType in election-candidates_v1.xsd
 */
data class CAND1Candidate(
    val forename: String,
    val surname: String,
    val citizenship: String,
    val candidateId: Int?,
    val candidateRegNumber: Int,
    val sequenceNumberInAdminUnit: Int?,
    val sequenceNumberInDistrict: Int?,
    val partyName: String,
    val education: String,
    val employment: String,
    val birthday: String,
    val sex: CAND1Sex,
    val contactInfos: List<CAND1ContactInfo> = emptyList(),
)

/**
 * Candidate sex enum
 * Maps to sex element restriction in election-candidates_v1.xsd
 */
enum class CAND1Sex {
    F,  // Female
    M,  // Male
}

/**
 * Contact information for candidate
 * Maps to contactInfoType in election-candidates_v1.xsd
 */
data class CAND1ContactInfo(
    val type: CAND1ContactType,
    val contact: String,
)

/**
 * Contact information type enum
 * Maps to type element restriction in election-candidates_v1.xsd
 */
enum class CAND1ContactType {
    PHONE,
    ADDRESS,
    EMAIL,
    FB,
    TWITTER,
    X_TWITTER,
}

