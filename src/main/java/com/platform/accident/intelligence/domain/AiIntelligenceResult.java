package com.platform.accident.intelligence.domain;

import java.util.List;

public record AiIntelligenceResult(
        String summary,
        EntityRegistry entities,
        String severityLevel,
        List<String> suggestedNextSteps,
        String rawAiOutput // Preserved for auditing/fallback
) {}
