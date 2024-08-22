package com.example.promo.exception;

public class CoinLessZeroException extends RuntimeException {
    public CoinLessZeroException(String message) {
        super(message);
    }
}
