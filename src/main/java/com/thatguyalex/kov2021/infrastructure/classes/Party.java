package com.thatguyalex.kov2021.infrastructure.classes;

import lombok.Data;

@Data
public class Party {
    private String name;
    private String code;
    private int votesCount;
    private float votePercentage;
}
