package com.thatguyalex.kov2021.Presentation;

import com.thatguyalex.kov2021.Presentation.classes.CountyData;
import com.thatguyalex.kov2021.Presentation.classes.PersonData;
import com.thatguyalex.kov2021.application.DataProcessor;
import com.thatguyalex.kov2021.infrastructure.DataProvider;
import com.thatguyalex.kov2021.infrastructure.classes.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class ElectionEndpoint {

    @Autowired
    DataProvider dataProvider;

    @Autowired
    DataProcessor dataProcessor;

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResultData test() {
        return dataProvider.getData();
    }

    @GetMapping(value = "/county", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CountyData> getCountyData() {
        return dataProcessor.getCountyData();
    }

    @GetMapping(value = "/people", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PersonData> getPeople() {
        return dataProcessor.getPeopleData();
    }

}
