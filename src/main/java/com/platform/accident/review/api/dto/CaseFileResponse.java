package com.platform.accident.review.api.dto;

import com.platform.accident.review.domain.ReviewStatus;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;

import java.util.Map;

public record CaseFileResponse(
        String caseId,
        ReviewStatus currentStatus,
        String assignedAgentId,
        AccidentSnapshotView accidentData,
        AiAnalysisView aiInsights,
        Map<String, Object> previousCorrections
) {}
