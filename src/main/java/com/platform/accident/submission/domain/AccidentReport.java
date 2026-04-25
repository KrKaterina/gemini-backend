package com.platform.accident.submission.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accidents")
public class AccidentReport {
    @Id
    private String id;

    @Indexed(unique = true)
    private String caseId;

    private String reporterId;
    private AccidentStatus status;
    private Instant createdAt;
    private Instant occurrenceTime;

    private Location location;
    private String rawDescription;

    // NOW: Simply stores IDs. Ownership is managed by Asset Module.
    private List<String> assetIds;

    private EnrichedContext contextData;
    private Map<String, Object> aiAnalysis;

    private Map<String, Object> workflow;
}
