package com.platform.accident.enrichment.client;

import com.platform.accident.enrichment.client.dto.OverpassResponse;
import com.platform.accident.enrichment.domain.RoadGeometry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class RoadAdapter {
    private final WebClient webClient = WebClient.create("https://overpass-api.de/api");

    public Mono<RoadGeometry> fetchRoadMetadata(double lat, double lng) {
        String query = String.format("[out:json];way(around:50,%f,%f)[highway];out tags;", lat, lng);

        return webClient.post().uri("/interpreter").bodyValue("data=" + query)
                .retrieve().bodyToMono(OverpassResponse.class)
                .timeout(Duration.ofMillis(4000))
                .map(res -> {
                    if (res.elements().isEmpty()) return new RoadGeometry("Off-road/Unknown", "N/A", "0");
                    var tags = res.elements().get(0).tags();
                    return new RoadGeometry(tags.getOrDefault("name", "Unknown Street"),
                            tags.get("highway"), tags.getOrDefault("maxspeed", "50"));
                })
                .doOnError(e -> log.error("Maps API failure: {}", e.getMessage()));
    }
}