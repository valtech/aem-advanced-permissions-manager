package com.valtech.aapm.restrictions;

public enum Operators {
    EQUALS("_EQUALS_"),
    GREATER_THAN_EQUALS("_GREATER_THAN_EQUALS_"),
    GREATER_THEN("_GREATER_THEN_"),
    LESS_THAN_EQUALS("_LESS_THAN_EQUALS_"),
    LESS_THEN("_LESS_THEN_");
    private final String value;
    Operators(String value) {
        this.value = value;
    }
    public String getValue() {
        return value;
    }
}
