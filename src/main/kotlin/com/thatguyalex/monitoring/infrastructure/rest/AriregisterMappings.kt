package com.thatguyalex.monitoring.infrastructure.rest

import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType
import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType.ADMINISTRATION
import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType.BUSINESS_OWNERSHIP
import com.thatguyalex.monitoring.infrastructure.MonitoringEntryConnectionType.EMPLOYMENT
import com.thatguyalex.monitoring.infrastructure.MonitoringEntryType

// TODO: expad by alaliik
val ARIREGISTER_LEGAL_FORM_TYPE_MAPPINGS: Map<String, MonitoringEntryType> = mapOf(
    "KOVAS" to MonitoringEntryType.GOV_KOV,
    "TRAS" to MonitoringEntryType.GOV,
    "AVOIG" to MonitoringEntryType.GOV,
    "ERAK" to MonitoringEntryType.PARTY,
    "SA" to MonitoringEntryType.PUBLIC_BODY,
)

val ARIREGISTER_ROLE_MAPPINGS: Map<String, MonitoringEntryConnectionType?> = mapOf(
    // --- BUSINESS_OWNERSHIP: owner / parent org / shareholder / member ---
    "KOAS" to BUSINESS_OWNERSHIP,   // Kõrgemalseisev asutus (Superior institution)
    "UOSAN" to BUSINESS_OWNERSHIP,  // Usaldusosanik (Limited partner)
    "TOSAN" to BUSINESS_OWNERSHIP,  // Täisosanik (General partner)
    "YHL" to BUSINESS_OWNERSHIP,    // Ühistu liige (Cooperative member)
    "YHLLV" to BUSINESS_OWNERSHIP,  // Lisavastutusega ühistu liige (Cooperative member with additional liability)
    "OSAN" to BUSINESS_OWNERSHIP,   // Osanik (Shareholder)
    "HUL" to BUSINESS_OWNERSHIP,    // Hooneühistu liige (Housing cooperative member)
    "A" to BUSINESS_OWNERSHIP,      // Asutaja (Founder)
    "B" to BUSINESS_OWNERSHIP,      // Asutaja (sissemakseta) (Founder without contribution)
    "W" to BUSINESS_OWNERSHIP,      // Tegelik kasusaaja (Beneficial owner)
    "S" to BUSINESS_OWNERSHIP,      // Aktsionär (Shareholder)
    "O" to BUSINESS_OWNERSHIP,      // Osanik (Partner)
    "K" to BUSINESS_OWNERSHIP,      // Kohaliku omavalitsuse liidu liige (Local government association member)
    "H" to BUSINESS_OWNERSHIP,      // Hooneühistu liige (Housing cooperative member)
    "L" to BUSINESS_OWNERSHIP,      // Liikmeskogudus (Member congregation)

    // --- ADMINISTRATION: individuals with significant control ---
    "FV" to ADMINISTRATION,         // Fondivalitseja (Fund manager)
    "ASES" to ADMINISTRATION,       // Asutuse esindusõiguslik isik (Institution's authorized representative)
    "ESIS" to ADMINISTRATION,       // Esindama õigustatud isik (Authorized representative)
    "HNKL" to ADMINISTRATION,       // Haldusnõukogu liige (Administrative board member)
    "VALIT" to ADMINISTRATION,      // Valitseja (Manager)
    "ESOI" to ADMINISTRATION,       // esindama õigustatud isik(ud) (Authorized representatives)
    "JUHE" to ADMINISTRATION,       // juhatuse esimees (Chairman of the board)
    "LIKVJ" to ADMINISTRATION,      // Juhatuse liikmest likvideerija (Board member as liquidator)
    "PROK" to ADMINISTRATION,       // Prokurist (Authorized signatory)
    "EUSOS" to ADMINISTRATION,      // Esindama volitatud usaldusosanik (Authorized limited partner representative)
    "JUHA" to ADMINISTRATION,       // juhatuse ainuliige (Sole board member)
    "JUHJ" to ADMINISTRATION,       // juhatuse liige (juhataja) (Board member / director)
    "JUHL" to ADMINISTRATION,       // Juhatuse liige (Board member)
    "EUSOS2" to ADMINISTRATION,     // Esindama volitatud usaldusosanik (Authorized limited partner representative)
    "ESIS2" to ADMINISTRATION,      // Esindama õigustatud isik (Authorized representative)
    "LIKV" to ADMINISTRATION,       // Likvideerija (Liquidator)
    "SJESI" to ADMINISTRATION,      // Äriühingu seadusjärgne esindaja (Legal representative of a company)
    "VFILJ" to ADMINISTRATION,      // Filiaali juhataja (Branch manager)
    "V" to ADMINISTRATION,          // Volitatud isik (Authorized person)
    "N" to ADMINISTRATION,          // Nõukogu liige (Supervisory board member)
    "E" to ADMINISTRATION,          // Nõukogu esimees (Chairman of the supervisory board)
    "R" to ADMINISTRATION,          // Revisjonikomisjoni liige (Audit committee member)

    // --- null: no direct mapping ---
    "ORP" to null,                  // Osade registripidaja (Share register keeper)
    "ETTEV" to null,                // ettevõtja (Entrepreneur)
    "DOKH" to null,                 // Dokumentide hoidja (Document keeper)
    "AJPH" to EMPLOYMENT,           // Ajutine pankrotihaldur likvideerija ülesannetes (Temporary bankruptcy trustee as liquidator)
    "PANTP" to null,                // Pandipidaja (Pledge holder)
    "PANKR" to EMPLOYMENT,          // Pankrotihaldur (Bankruptcy trustee)
    "KISIK" to EMPLOYMENT,          // Kontaktisik (Contact person)
    "TVH" to null,                  // Tagatisvara haldur (Reserve asset manager)
    "YFI" to null,                  // Ühendav füüsiline isik (Connecting natural person)
    "MORAH" to null,                // Moratooriumihaldur (Moratorium administrator)
    "JPNKR" to null,                // Järelevalveõiguslik pankrotihaldur (Supervisory bankruptcy trustee)
    "ERIH" to null,                 // Erirežiimihaldur (Special regime administrator)
    "ARP" to null,                  // Aktsiaraamatu pidaja (Share register keeper)
    "AJUTPH" to EMPLOYMENT,         // Ajutine pankrotihaldur (Temporary bankruptcy trustee)
    "MDKPI" to null,                // Menetlusdokumentide kättesaamiseks pädev isik (Person for procedural documents)
    "FIE" to EMPLOYMENT,            // Füüsilisest isikust ettevõtja (Sole proprietor)
    "M" to null,                    // Mitterahalist sissemakset hinnanud audiitor (Auditor for non-monetary contributions)
    "P" to null,                    // Pankrotitoimkonna liige (Bankruptcy committee member)
    "J" to EMPLOYMENT,                    // Revident (Auditor)
    "D" to null,                    // Audiitorettevõtja (Audit company)
)

val ARIREGISTER_CONTACT_TYPE_MAPPINGS: Map<String, String> = mapOf(
    "WWW" to "website",   // Interneti WWW aadress
    "TEL" to "phone",     // Telefon
    "EMAIL" to "email",   // Elektronposti aadress
    "AMAIL" to "email",   // Ametlik Eesti e-mail
    "FAX" to "phone",     // Faks
    "MOB" to "phone",     // Mobiiltelefon
    "MOD" to "other",     // Modem
    "TELEX" to "other",   // Teleks
    "MUU" to "other",     // Muu sidevahend (Other communication device)
)

val ARIREGISTER_LEGAL_FORM_SUFFIXES: Set<String> = setOf(
    // code -> name
    "MPÜ",    "Maaparandusühistu",
    "FIE",    "Füüsilisest isikust ettevõtja",
    "MTÜ",    "Mittetulundusühing",
    "SA",     "Sihtasutus",
    "FA",     "Välismaa äriühingu filiaal", "Filiaal",
    "HÜ",    "Tulundusühistu",
    "TRAS",   "Täidesaatva riigivõimu asutus või riigi muu institutsioon",
    "AVOIG",  "Avalik-õiguslik juriidiline isik, põhiseaduslik institutsioon või nende asutus",
    "KOVAS",  "Kohaliku omavalitsuse asutus",
    "KÜ",    "Korteriühistu",
    "AMETÜ", "Ametiühing",
    "UÜ",    "Usaldusühing",
    "AS",     "Aktsiaselts",
    "TÜ",    "Täisühing",
    "OÜ",    "Osaühing",
    "TÜH",   "Tulundusühistu",
    "EMÜ",   "Euroopa majandushuviühing",
    "SE",     "Euroopa äriühing (Societas Europaea)",
    "SCE",    "Euroopa ühistu",
    "TKR",    "Euroopa territoriaalse koostöö rühmitus",
    "FIL",    "Välismaa äriühingu filiaal",
    "ERAK",   "Erakond",
)
