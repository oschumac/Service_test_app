package com.example.admin.service_test_app.display.parser;

/**
 * Created by fishermen21 on 21.05.17.
 */

public class CharacterPattern extends Pattern {
    private final char character;

    public CharacterPattern(char c, String[] patternString, int blocksize) {
        super(patternString,blocksize);
        this.character = c;
    }

    @Override
    public String toString() {
        return "Character("+character+")";
    }

    public char getCharacter() {
        return character;
    }
}
