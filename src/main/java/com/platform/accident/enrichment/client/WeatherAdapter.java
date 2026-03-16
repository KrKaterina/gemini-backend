package com.platform.accident.enrichment.client;

import com.platform.accident.enrichment.client.dto.WeatherResponse;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class WeatherAdapter {
    private final WebClient webClient = WebClient.create("https://archive-api.open-meteo.com/v1");

    public Mono<WeatherMetrics> fetchHistoricalWeather(double lat, double lng, Instant timestamp) {
        String date = timestamp.atOffset(ZoneOffset.UTC).toLocalDate().toString();
        int hour = timestamp.atOffset(ZoneOffset.UTC).getHour();

        return webClient.get()
                .uri(u -> u.path("/archive").queryParam("latitude", lat).queryParam("longitude", lng)
                        .queryParam("start_date", date).queryParam("end_date", date)
                        .queryParam("hourly", "temperature_2m,weather_code").build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .timeout(Duration.ofMillis(2500))
                .map(res -> {
                    Double temp = res.hourly().temperatures().get(hour);
                    Integer code = res.hourly().weatherCodes().get(hour);
                    return new WeatherMetrics("CODE_" + code, temp);
                })
                .doOnError(e -> log.error("Weather API failure: {}", e.getMessage()));
    }

    public Mono<WeatherMetrics> fetchWeatherAtTimestamp(double lat, double lng, Instant timestamp) {
        String date = timestamp.atOffset(ZoneOffset.UTC).toLocalDate().toString();
        int hour = timestamp.atOffset(ZoneOffset.UTC).getHour();
        int minute = timestamp.atOffset(ZoneOffset.UTC).getMinute();

        return webClient.get()
                .uri(u -> u.path("/archive")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lng)
                        .queryParam("start_date", date)
                        .queryParam("end_date", date)
                        .queryParam("hourly", "temperature_2m,weather_code")
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .map(res -> {
                    // interpolation για το λεπτό
                    double tempHour = res.hourly().temperatures().get(hour);
                    double tempNextHour = res.hourly().temperatures().get(hour + 1);
                    double interpolatedTemp = tempHour + (tempNextHour - tempHour) * (minute / 60.0);

                    int weatherCode = res.hourly().weatherCodes().get(hour);
                    return new WeatherMetrics("CODE_" + weatherCode, interpolatedTemp);
                })
                .doOnError(e -> log.error("Weather API failure at timestamp {}: {}", timestamp, e.getMessage()));
    }
}