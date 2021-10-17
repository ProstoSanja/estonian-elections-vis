package com.thatguyalex.kov2021.Presentation.classes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PartyData {
    private String name;
    private int votes;
}
