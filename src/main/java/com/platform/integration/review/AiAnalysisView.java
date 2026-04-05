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
) {}