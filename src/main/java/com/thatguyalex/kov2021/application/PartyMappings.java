package com.thatguyalex.kov2021.application;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PartyMappings {

    public String getPartyShortCode(String partyName) {
        return switch(partyName) {
            case "Eesti Keskerakond" -> "KESK";
            case "Eesti Reformierakond" -> "REF";
            case "Sotsiaaldemokraatlik Erakond" -> "SDE";
            case "Erakond Isamaa ja Res Publica Liit" -> "IRL";
            case "Eesti Konservatiivne Rahvaerakond" -> "EKRE";
            case "Erakond Eestimaa Rohelised" -> "ROH";
            default -> partyName;
        };
    }
    
}
