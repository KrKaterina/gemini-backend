package com.platform.accident.enrichment.repository;

import com.platform.accident.enrichment.domain.RoadGeometry;
import com.platform.accident.enrichment.domain.WeatherMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "external_context_cache")
public class ExternalContextCache {
    @Id
    private String id;

    @Indexed private String geoHash;
    @Indexed private Instant referenceTime;

    private WeatherMetrics weatherData;
    private RoadGeometry roadData;

    @Indexed(expireAfterSeconds = 86400) // TTL 24h
    private Instant expiresAt;
}
