package com.thatguyalex.kov2021.Presentation.classes;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CountyData {
    private String id;
    private int totalVotes;
    private int ballotStations;
    private int ballotStationsCounted;
    private String leadingParty;
    private List<PartyData> partyData;
}
