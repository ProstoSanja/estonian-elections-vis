package com.thatguyalex.kov2021.Presentation;

import com.thatguyalex.kov2021.Presentation.classes.CountyData;
import com.thatguyalex.kov2021.Presentation.classes.PartyData;
import com.thatguyalex.kov2021.infrastructure.DataProvider;
import com.thatguyalex.kov2021.infrastructure.classes.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
public class ElectionEndpoint {

    @Autowired
    DataProvider dataProvider;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultData test() {
        return dataProvider.getData();
    }

    @GetMapping(value = "/byCounty", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountyData> getByCounty() {
        return dataProvider.getData().getData().getResultsInAdminUnit().stream().map(it -> {
            List<PartyData> partyData = null;
            if (it.getParties().getParty() != null) {
                partyData = it.getParties().getParty().stream().map(party -> new PartyData(party.getCode(), party.getVotesCount())).sorted(Comparator.comparingInt(PartyData::getVotes)).toList();
            }
            return CountyData.builder()
                .id(it.getAdminUnit().getCode())
                .totalVotes(it.getTotalVotes())
                .leadingParty(partyData != null ? partyData.get(0).getName() : null)
                .partyData(partyData)
                .build();
            })
            .toList();
    }

}
