package com.thatguyalex.kov2021.Presentation.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PersonData {
    private String name;
    private String lastname;
    private int regNumber;
    private int totalVotes;
    private String partyName;
    private boolean elected;
}
