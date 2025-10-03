package com.taskManagers.taskapp;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CommandParser {

    public Command parse(String input) {
        if (input == null) {
            return new Command(CommandType.BACK);
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return new Command(CommandType.BACK);
        }
        String lower = trimmed.toLowerCase(Locale.US);
        switch (lower) {
            case "a", "add" -> {
                return new Command(CommandType.ADD);
            }
            case "e", "edit" -> {
                return new Command(CommandType.EDIT);
            }
            case "d", "done" -> {
                return new Command(CommandType.DONE);
            }
            case "t", "today" -> {
                return new Command(CommandType.TODAY);
            }
            case "q", "quit", "exit" -> {
                return new Command(CommandType.QUIT);
            }
            case "?", "help" -> {
                return new Command(CommandType.HELP);
            }
            case "n", "next" -> {
                return new Command(CommandType.NEXT_PAGE);
            }
            case "p", "prev", "previous" -> {
                return new Command(CommandType.PREVIOUS_PAGE);
            }
            case "s" -> {
                return new Command(CommandType.SEARCH, "");
            }
            default -> {
                if (lower.startsWith("search ")) {
                    return new Command(CommandType.SEARCH, trimmed.substring(7).trim());
                }
                if (lower.startsWith("filter ")) {
                    Map<String, String> options = parseOptions(trimmed.substring(7).trim());
                    return new Command(CommandType.FILTER, "", options);
                }
                return new Command(CommandType.UNKNOWN, trimmed);
            }
        }
    }

    private Map<String, String> parseOptions(String text) {
        Map<String, String> options = new HashMap<>();
        if (text == null || text.isBlank()) {
            return options;
        }
        String[] tokens = text.split("\\s+");
        for (String token : tokens) {
            int idx = token.indexOf('=');
            if (idx > 0 && idx < token.length() - 1) {
                String key = token.substring(0, idx).trim().toLowerCase(Locale.US);
                String value = token.substring(idx + 1).trim();
                options.put(key, value);
            }
        }
        return options;
    }
}
