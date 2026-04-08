package com.thatguyalex.monitoring.infrastructure

enum class MonitoringEntryType {
    INDIVIDUAL,
    PARTY,
    GOV,
    GOV_KOV,
    BUSINESS,
    PUBLIC_BODY,
}

enum class MonitoringEntryConnectionType {
    PARTY_MEMBERSHIP,
    BUSINESS_OWNERSHIP,
    EMPLOYMENT,
    ADMINISTRATION,
    DONATION,
    BANK_LOAN,
    UNKNOWN,
}

enum class MonitoringExternalIdType {
    EST_GOV_ID,
    ARIREGISTER_ANON_ID,
    ERJK_ID,
    RIIGIKOGU_GUID,
}
