package com.thatguyalex.kov2021.Presentation;

import com.thatguyalex.kov2021.Presentation.classes.CountyData;
import com.thatguyalex.kov2021.Presentation.classes.PersonData;
import com.thatguyalex.kov2021.application.DataProcessor;
import com.thatguyalex.kov2021.infrastructure.DataProvider;
import com.thatguyalex.kov2021.infrastructure.classes.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(value = "/api")
public class ElectionEndpoint {

    @Autowired
    DataProvider dataProvider;

    @Autowired
    DataProcessor dataProcessor;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultData test() {
        return dataProvider.getResultData(); //TODO: remove me before prod
    }

    @GetMapping(value = "/countyData", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountyData> getCountyData() {
        return dataProcessor.getCountyData();
    }

    @GetMapping(value = "/topPeopleData", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PersonData> getTopPeople() {
        return dataProcessor.getTopPeopleData();
    }

    @GetMapping(value = "/person/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PersonData> getPeopleList() {
        return dataProcessor.getAllPeople();
    }

    @GetMapping(value = "/person", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PersonData> getPeopleList(@RequestParam List<String> names) {
        if (names.size() > 50) {
            names = names.subList(0,50);
        }
        return dataProcessor.getPersonById(names);
    }


}
