package com.thatguyalex.rk2023.infrastructure.classes.helpers

private val CHARACTER_FOLDING_MAP = mapOf(
    'ò' to 'o', 'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o', 'ø' to 'o',
    'à' to 'a', 'á' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a', 'å' to 'a',
    'ù' to 'u', 'ú' to 'u', 'û' to 'u', 'ü' to 'u',
    'š' to 's', 'ś' to 's', 'ŝ' to 's', 'ş' to 's', 'ș' to 's',
    'ž' to 'z', 'ź' to 'z', 'ż' to 'z', 'ẑ' to 'z'
)

fun tokenizeString(str: String): String = buildString(str.length) {
    str.lowercase().forEach { char ->
        val folded = CHARACTER_FOLDING_MAP[char] ?: char
        if (folded.isLetterOrDigit()) {
            append(folded)
        }
    }
}

private val ultraShortPartyCodeLookup = mapOf(
    "IE" to "IE",
    "SDE" to "SD",
    "EKRE" to "EK",
    "REF" to "RE",
    "KESK" to "KE",
    "EE200" to "EE",
    "PP" to "PP",
    "EERK" to "ER",
    "ROH" to "RO",
    "VAL_LIIDUD" to "VL"
)

private val veryShortPartyCodeLookup = mapOf(
    "IE" to "ISA",
    "SDE" to "SDE",
    "EKRE" to "EKRE",
    "REF" to "REF",
    "KESK" to "KESK",
    "EE200" to "200",
    "PP" to "PP",
    "EERK" to "ERK",
    "ROH" to "ROH",
    "VAL_LIIDUD" to "VAL"
)

enum class PartyCodeType {
    ULTRA, VERY
}

fun getShortPartyCode(partyCode: String, type: PartyCodeType = PartyCodeType.ULTRA): String {
    return when (type) {
        PartyCodeType.ULTRA -> {
            ultraShortPartyCodeLookup[partyCode] 
                ?: partyCode.replace(Regex("^(VL|V)"), "").take(2)
        }
        PartyCodeType.VERY -> {
            veryShortPartyCodeLookup[partyCode] 
                ?: partyCode.replace(Regex("^(VL|V)"), "").take(3)
        }
    }
}