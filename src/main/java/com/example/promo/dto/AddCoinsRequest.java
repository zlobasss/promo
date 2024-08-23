package com.example.promo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddCoinsRequest {
    private String vkId;
    private int coins;
}
