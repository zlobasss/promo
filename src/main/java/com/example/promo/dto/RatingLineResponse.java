package com.example.promo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RatingLineResponse {
    private Long rank;
    private String firstAndLastName;
    private int coins;
}
