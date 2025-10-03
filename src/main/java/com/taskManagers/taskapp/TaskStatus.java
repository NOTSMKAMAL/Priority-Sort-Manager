package com.taskManagers.taskapp;

public enum TaskStatus {
    OPEN("NOT DONE"),
    DONE("DONE");

    private final String dbValue;

    TaskStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String dbValue() {
        return dbValue;
    }

    public static TaskStatus fromDb(String value) {
        if (value == null) {
            return OPEN;
        }
        String normalized = value.trim().toUpperCase();
        for (TaskStatus status : values()) {
            if (status.dbValue.equalsIgnoreCase(normalized) || status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return OPEN;
    }
}
