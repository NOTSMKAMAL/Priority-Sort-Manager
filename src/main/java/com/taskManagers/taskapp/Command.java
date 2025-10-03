package com.taskManagers.taskapp;

import java.util.Collections;
import java.util.Map;

public record Command(CommandType type, String text, Map<String, String> options) {
    public Command(CommandType type) {
        this(type, "", Collections.emptyMap());
    }

    public Command(CommandType type, String text) {
        this(type, text, Collections.emptyMap());
    }
}
