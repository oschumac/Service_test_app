package com.example.admin.service_test_app.display.parser;

/**
 * Created by fishermen21 on 21.05.17.
 */

public class NumberPattern extends Pattern {
    private final int number;

    public NumberPattern(int number, String[] patternString, int blockSize) {
        super(patternString,blockSize);
        this.number = number;
    }

    @Override
    public String toString() {
        return "Numbervalue("+number+")";
    }

    public int getNumber() {
        return number;
    }
}
