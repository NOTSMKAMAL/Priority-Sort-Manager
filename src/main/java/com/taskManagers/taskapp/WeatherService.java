package com.taskManagers.taskapp;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeatherService {

    private static final Pattern OW_TEMP_PATTERN = Pattern.compile("\"temp\"\\s*:\\s*([\\d.]+)");
    private static final Pattern OW_DESC_PATTERN = Pattern.compile("\"description\"\\s*:\\s*\"([^\\\"]+)\"");
    private static final Pattern WTTR_CURRENT_PATTERN = Pattern.compile("\"current_condition\"\\s*:\\s*\\[\\s*\\{[^}]*?\"temp_F\"\\s*:\\s*\"([-\\d]+)\"[^}]*?\"weatherDesc\"\\s*:\\s*\\[\\s*\\{\\s*\"value\"\\s*:\\s*\"([^\\\"]+)\"");

    private static final Map<String, Integer> FALLBACK_HIGHS_F = Map.of(
            normalizeCity("Riverside,CA"), 92,
            normalizeCity("Los Angeles,CA"), 85
    );

    private final TaskAppConfig config;
    private final HttpClient client;

    public WeatherService(TaskAppConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public String fetch() {
        List<String> cities = resolveCities();
        List<String> summaries = new ArrayList<>();
        boolean anyObserved = false;
        for (String city : cities) {
            Optional<Observation> observation = fetchWeather(city);
            if (observation.isPresent()) {
                summaries.add(formatDisplay(city, observation.get()));
                anyObserved = true;
                continue;
            }

            Optional<String> fallback = fallbackHighSummary(city);
            if (fallback.isPresent()) {
                summaries.add(fallback.get());
                anyObserved = true;
            } else {
                summaries.add(String.format(Locale.US, "%s unavailable", prettyCityName(city)));
            }
        }
        if (anyObserved || !summaries.isEmpty()) {
            return "Weather: " + String.join(" | ", summaries);
        }
        return "Weather: unavailable";
    }

    private List<String> resolveCities() {
        List<String> values = new ArrayList<>();
        String configured = config.city();
        if (configured != null && !configured.isBlank()) {
            String[] tokens = configured.split("[;|]");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
            if (!values.isEmpty()) {
                return values;
            }
            values.add(configured.trim());
            return values;
        }
        values.add("Riverside,CA");
        values.add("Los Angeles,CA");
        return values;
    }

    private Optional<Observation> fetchWeather(String city) {
        if (config.hasWeatherConfig()) {
            Optional<Observation> ow = requestOpenWeather(city);
            if (ow.isPresent()) {
                return ow;
            }
        }
        return requestWttr(city);
    }

    private Optional<Observation> requestOpenWeather(String city) {
        try {
            URI uri = URI.create(String.format(Locale.US,
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=imperial",
                    encode(city), encode(config.weatherApiKey())));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseOpenWeather(response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalArgumentException ex) {
            // ignore and fall back
        }
        return Optional.empty();
    }

    private Optional<Observation> requestWttr(String city) {
        try {
            URI uri = URI.create(String.format(Locale.US,
                    "https://wttr.in/%s?format=j1",
                    encode(city)));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return parseWttr(response.body());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalArgumentException ex) {
            // ignore and fall back
        }
        return Optional.empty();
    }

    private Optional<Observation> parseOpenWeather(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        Matcher tempMatcher = OW_TEMP_PATTERN.matcher(body);
        Matcher descMatcher = OW_DESC_PATTERN.matcher(body);
        if (tempMatcher.find() && descMatcher.find()) {
            double temp = Double.parseDouble(tempMatcher.group(1));
            long rounded = Math.round(temp);
            String description = descMatcher.group(1);
            return Optional.of(new Observation(rounded, description));
        }
        return Optional.empty();
    }

    private Optional<Observation> parseWttr(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = WTTR_CURRENT_PATTERN.matcher(body);
        if (matcher.find()) {
            try {
                long temperature = Long.parseLong(matcher.group(1));
                String description = matcher.group(2);
                return Optional.of(new Observation(temperature, description));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private Optional<String> fallbackHighSummary(String city) {
        Integer high = FALLBACK_HIGHS_F.get(normalizeCity(city));
        if (high == null) {
            return Optional.empty();
        }
        return Optional.of(String.format(Locale.US, "%s high %d\u00B0F (offline)",
                prettyCityName(city),
                high));
    }

    private String formatDisplay(String city, Observation observation) {
        return String.format(Locale.US, "%s %d\u00B0F, %s",
                prettyCityName(city),
                observation.temperatureF(),
                capitalize(observation.description()));
    }

    private String prettyCityName(String city) {
        String trimmed = city.trim();
        int comma = trimmed.indexOf(',');
        if (comma > -1 && comma < trimmed.length() - 1) {
            return trimmed.substring(0, comma).trim() + ", " + trimmed.substring(comma + 1).trim();
        }
        return trimmed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String normalizeCity(String city) {
        return city.trim().toLowerCase(Locale.US);
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private record Observation(long temperatureF, String description) { }
}

