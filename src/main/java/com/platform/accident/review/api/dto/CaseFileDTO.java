package com.platform.accident.review.api.dto;

import com.platform.accident.submission.domain.Location;

import java.util.List;

public record CaseFileDTO(
        // Accident Summary
        String caseId,
        String reporterId,
        String description,
        Location location,
        String environment, // Combined Weather + Road

        // Intelligence Insights
        String aiSummary,
        String aiSeverity,
        List<String> suggestedSteps,

        // Human Workflow State
        String lockStatus,
        String currentAgentName,

        // Media discovery
        List<String> mediaUrls
) {}