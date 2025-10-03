package com.taskManagers.taskapp;

public enum PriorityLevel {
    P1(1, "HIGH"),
    P2(2, "MEDIUM"),
    P3(3, "LOW");

    private final int weight;
    private final String dbValue;

    PriorityLevel(int weight, String dbValue) {
        this.weight = weight;
        this.dbValue = dbValue;
    }

    public int weight() {
        return weight;
    }

    public String dbValue() {
        return dbValue;
    }

    public static PriorityLevel fromDb(String value) {
        if (value == null) {
            return P3;
        }
        String normalized = value.trim().toUpperCase();
        for (PriorityLevel level : values()) {
            if (level.dbValue.equalsIgnoreCase(normalized) || level.name().equalsIgnoreCase(normalized)) {
                return level;
            }
        }
        return switch (normalized) {
            case "P1" -> P1;
            case "P2" -> P2;
            case "P3" -> P3;
            default -> P3;
        };
    }
}
