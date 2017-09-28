package com.example.admin.service_test_app.display.parser;

import com.example.admin.service_test_app.display.Symbol;

/**
 * Created by fishermen21 on 21.05.17.
 */

public class SymbolPattern extends Pattern {

    private final Symbol symbol;
    public SymbolPattern(Symbol s, String[] patternString, int blocksize) {
        super(patternString, blocksize);
        this.symbol = s;

    }

    @Override
    public String toString() {
        return "Symbol("+symbol+")";
    }

    public Symbol getSymbol() {
        return symbol;
    }
}
