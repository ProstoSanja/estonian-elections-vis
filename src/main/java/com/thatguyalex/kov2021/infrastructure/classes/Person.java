package com.thatguyalex.kov2021.infrastructure.classes;

import lombok.Data;

@Data
public class Person {
    private String forename;
    private String surname;
    private int candidateRegNumber;
    private int sequenceNumberInAdminUnit;
    private int sequenceNumberInDistrict;
    private int votesCount;
    private int seqNoFinal;
    private String partyName;
    private int districtNumber;
    private float quota;
    private float vrd;
    private boolean v1;
    private boolean v2;
    private boolean v3;
    private boolean elected;
    private boolean reserved;
}
