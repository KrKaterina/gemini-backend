package com.platform.accident.enrichment.client;

import com.platform.accident.enrichment.client.dto.OverpassResponse;
import com.platform.accident.enrichment.domain.RoadGeometry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Locale;

@Component
@Slf4j
public class RoadAdapter {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://overpass-api.de/api")
            .defaultHeader("User-Agent", "PostmanRuntime/7.32.3")
            .build();

    public Mono<RoadGeometry> fetchRoadMetadata(double lat, double lng) {

        String query = String.format(Locale.US, "[out:json];(way(around:150,%f,%f)[\"highway\"];);out body;", lat, lng);

        log.info("EXACT QUERY: {}", query);

        return webClient.post()
                .uri("/interpreter")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("data", query))
                .retrieve()
                .bodyToMono(OverpassResponse.class)
                .timeout(Duration.ofSeconds(20))
                .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(2)))
                .map(res -> {
                    if (res == null || res.elements() == null || res.elements().isEmpty()) {
                        log.warn("Overpass: No elements found for {}, {}", lat, lng);
                        return new RoadGeometry("Off-road Area", "unpaved", "0");
                    }

                    return res.elements().stream()
                            .filter(e -> e.tags() != null && !e.tags().isEmpty())
                            .findFirst()
                            .map(e -> {
                                var tags = e.tags();

                                String name = tags.getOrDefault("name",
                                        tags.getOrDefault("addr:street",
                                                tags.getOrDefault("int_name", "Local Access Road")));

                                String type = tags.getOrDefault("highway", "unknown");

                                String speed = tags.get("maxspeed");

                                if (speed == null) {
                                    speed = switch (type) {
                                        case "motorway" -> "120";
                                        case "trunk" -> "90";
                                        case "primary" -> "50";
                                        case "secondary", "tertiary" -> "50";
                                        case "residential" -> "30";
                                        case "living_street" -> "20";
                                        case "service" -> "10";
                                        case "track" -> "20";
                                        default -> "50";
                                    };
                                    log.info("ENRICHMENT: Using heuristic speed limit {} for road type {}", speed, type);
                                } else {
                                    speed = speed.replaceAll("[^0-9]", "");
                                }

                                log.info("TELEMETRY SYNCHRONIZED: {} ({}) @ {} km/h", name, type, speed);
                                return new RoadGeometry(name, type, speed);
                            })
                            .orElse(new RoadGeometry("Unnamed Structure", "unknown", "50"));
                });
    }
}