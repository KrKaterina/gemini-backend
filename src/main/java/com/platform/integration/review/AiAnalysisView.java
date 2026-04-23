package com.platform.integration.review;

import java.util.List;

/**
 * Data Snapshot from Module 3 (Intelligence) for the Human Agent dashboard.
 */
public record AiAnalysisView(
        String summary,
        String severityLevel,
        List<String> suggestedNextSteps,
        String rawAiOutput
) {
    public static AiAnalysisView empty() {
        return new AiAnalysisView("Processing not completed", "N/A", java.util.List.of(), "System trace active");
    }
}