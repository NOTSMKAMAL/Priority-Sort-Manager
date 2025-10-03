package com.taskManagers.taskapp;

public final class AnsiColors {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String HEADER = "\u001B[95m";
    public static final String SUBTLE = "\u001B[90m";
    public static final String SUCCESS = "\u001B[92m";
    public static final String WARN = "\u001B[93m";
    public static final String ERROR = "\u001B[91m";
    public static final String ACCENT = "\u001B[96m";

    private AnsiColors() {
    }

    public static String format(String color, String value, boolean enabled) {
        if (!enabled || color == null || color.isBlank()) {
            return value;
        }
        return color + value + RESET;
    }
}
