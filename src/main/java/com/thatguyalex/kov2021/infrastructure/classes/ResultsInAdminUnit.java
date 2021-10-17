package com.thatguyalex.kov2021.infrastructure.classes;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "resultsInAdminUnit")
public class ResultsInAdminUnit {
    private AdminUnit adminUnit;
    private int totalDepartmentsCount;
    private int confirmedDepartmentCount;
    private int totalVotes;
    private boolean isEVOTINGConfirmed;
    private PartyList parties;
    private PersonList electedPersons;

    @Data
    public static class PartyList{
        private List<Party> party;
    }

    @Data
    public static class PersonList{
        private List<Person> person;
    }
}
