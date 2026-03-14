package com.platform.accident.enrichment.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WeatherResponse(Hourly hourly) {
    public record Hourly(
            @JsonProperty("temperature_2m") List<Double> temperatures,
            @JsonProperty("weather_code") List<Integer> weatherCodes
    ) {}
}
