package com.platform.accident.intelligence.repository;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "ai_analysis_logs")
public class AiAnalysisLog {
    @Id
    private String id;

    @Indexed // Critical for finding logs associated with a case
    private String caseId;

    private String modelId;
    private Instant processedAt;
    private long durationMs;
    private boolean successful;
    private String errorDetail;

    private String sentPrompt;
}