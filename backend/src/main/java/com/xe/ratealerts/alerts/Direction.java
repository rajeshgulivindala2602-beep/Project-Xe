package com.xe.ratealerts.alerts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Direction {
    ABOVE,
    BELOW;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Direction fromJson(String value) {
        return switch (value == null ? "" : value) {
            case "above" -> ABOVE;
            case "below" -> BELOW;
            default -> throw new IllegalArgumentException("Direction must be 'above' or 'below'.");
        };
    }
}
