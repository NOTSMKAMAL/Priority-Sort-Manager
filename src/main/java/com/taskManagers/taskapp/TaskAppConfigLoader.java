package com.taskManagers.taskapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class TaskAppConfigLoader {

    private static final Set<String> BOOLEAN_FLAGS = Set.of("no-color", "no_color");

    private TaskAppConfigLoader() {
    }

    public static TaskAppConfig load(String[] args) {
        Map<String, String> cli = parseArgs(args);
        boolean explicitConfig = cli.containsKey("config");
        String requestedConfig = cli.getOrDefault("config", ".env");
        String configPath = expandHomeDirectory(requestedConfig);

        Map<String, String> merged = new HashMap<>(System.getenv());
        loadEnvFile(configPath, explicitConfig).forEach((key, value) -> merged.putIfAbsent(key, value));

        boolean noColorFlag = cli.containsKey("no-color") || cli.containsKey("no_color")
                || toBoolean(merged.getOrDefault("NO_COLOR", "false"));
        boolean colorEnabled = !noColorFlag;

        String dbUrlEnv = valueOrDefault(merged, "DB_URL", "");
        String dbUserEnv = valueOrDefault(merged, "DB_USER", "root");
        String dbPasswordEnv = valueOrDefault(merged, "DB_PASS", "");
        String weatherKeyEnv = valueOrDefault(merged, "OPENWEATHER_API_KEY", "");
        String cityEnv = valueOrDefault(merged, "CITY", "Riverside,CA;Los Angeles,CA");

        String dbUrl = defaulted(fromCli(cli, "db-url", "db_url"), dbUrlEnv);
        String dbUser = defaulted(fromCli(cli, "db-user", "db_user"), dbUserEnv);
        String dbPassword = defaulted(fromCli(cli, "db-pass", "db_password"), dbPasswordEnv);
        String weatherKey = defaulted(fromCli(cli, "openweather_api_key", "weather-key", "weather_key"), weatherKeyEnv);
        String city = defaulted(fromCli(cli, "city"), cityEnv);

        return new TaskAppConfig(dbUrl, dbUser, dbPassword, weatherKey, city, colorEnabled, configPath);
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        if (args == null) {
            return parsed;
        }
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                continue;
            }
            String stripped = arg.substring(2);
            if (stripped.contains("=")) {
                String[] parts = stripped.split("=", 2);
                parsed.put(parts[0].toLowerCase(), parts[1]);
            } else {
                String key = stripped.toLowerCase();
                if (BOOLEAN_FLAGS.contains(key)) {
                    parsed.put(key, "true");
                    continue;
                }
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    parsed.put(key, args[++i]);
                } else {
                    parsed.put(key, "");
                }
            }
        }
        return parsed;
    }

    private static Map<String, String> loadEnvFile(String configPath, boolean explicit) {
        if (configPath == null || configPath.isBlank()) {
            return Map.of();
        }
        Path path = Path.of(configPath);
        if (!Files.exists(path)) {
            if (explicit) {
                System.err.println("Warning: config file " + configPath + " not found.");
            }
            return Map.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(TaskAppConfigLoader::splitEnvLine)
                    .filter(entry -> entry.length == 2)
                    .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1], (a, b) -> b));
        } catch (IOException ex) {
            System.err.println("Warning: unable to read config file " + configPath + ": " + ex.getMessage());
            return Map.of();
        }
    }

    private static String[] splitEnvLine(String line) {
        int idx = line.indexOf('=');
        if (idx <= 0) {
            return new String[0];
        }
        String key = line.substring(0, idx).trim();
        String value = line.substring(idx + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }
        return new String[]{key, value};
    }

    private static String fromCli(Map<String, String> cli, String... keys) {
        if (cli == null || cli.isEmpty()) {
            return null;
        }
        for (String key : keys) {
            String value = cli.get(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String defaulted(String primary, String fallback) {
        if (primary == null || primary.isBlank()) {
            return fallback;
        }
        return primary.trim();
    }

    private static String valueOrDefault(Map<String, String> source, String key, String fallback) {
        String value = source.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static boolean toBoolean(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("1") || normalized.equals("true") || normalized.equals("yes");
    }

    private static String expandHomeDirectory(String path) {
        if (path == null || path.isBlank() || path.charAt(0) != '~') {
            return path;
        }
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return path;
        }
        if (path.length() == 1) {
            return home;
        }
        return Path.of(home, path.substring(1)).toString();
    }
}

