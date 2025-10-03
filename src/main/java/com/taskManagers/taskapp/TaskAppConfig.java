package com.taskManagers.taskapp;

public record TaskAppConfig(
        String dbUrl,
        String dbUser,
        String dbPassword,
        String weatherApiKey,
        String city,
        boolean colorEnabled,
        String configPath) {

    public boolean hasWeatherConfig() {
        return weatherApiKey != null && !weatherApiKey.isBlank()
                && city != null && !city.isBlank();
    }
}
