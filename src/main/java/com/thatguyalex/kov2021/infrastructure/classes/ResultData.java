package com.thatguyalex.kov2021.infrastructure.classes;

import lombok.Data;

import javax.xml.bind.annotation.*;
import java.util.List;

@Data
@XmlRootElement(name = "resultsOutput")
public class ResultData{
        private String electionCode;
        private String reportType;
        private String generated;
        private ResultsInAdminUnitList data;

        @Data
        public static class ResultsInAdminUnitList{
                private List<ResultsInAdminUnit> resultsInAdminUnit;
        }
}
