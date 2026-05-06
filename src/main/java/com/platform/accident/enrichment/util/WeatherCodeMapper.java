package com.platform.accident.enrichment.util;

import java.util.Map;

public class WeatherCodeMapper {
    private static final Map<Integer, String> CODE_MAP = Map.ofEntries(
            Map.entry(0, "Clear sky"),
            Map.entry(1, "Mainly clear"),
            Map.entry(2, "Partly cloudy"),
            Map.entry(3, "Overcast"),
            Map.entry(45, "Fog"),
            Map.entry(48, "Depositing rime fog"),
            Map.entry(51, "Light drizzle"),
            Map.entry(61, "Light rain"),
            Map.entry(63, "Moderate rain"),
            Map.entry(65, "Heavy rain"),
            Map.entry(80, "Slight rain showers"),
            Map.entry(95, "Thunderstorm")
    );

    public static String getDescription(int code) {
        return CODE_MAP.getOrDefault(code, "Unknown Weather (Code " + code + ")");
    }

    public static String translate(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return "Unknown conditions";

        if (rawCode.startsWith("CODE_")) {
            try {
                int code = Integer.parseInt(rawCode.replace("CODE_", ""));
                return CODE_MAP.getOrDefault(code, "Unknown conditions");
            } catch (Exception e) {
                return "Unspecified weather";
            }
        }

        return rawCode;
    }

}