package com.platform.accident.review.api.dto;

import com.platform.integration.review.*;
import com.platform.integration.media.MediaMetadataView;
import java.util.List;

public record ConsolidatedCaseFile(
        String caseId,
        String status,
        AccidentSnapshotView accidentDetails,
        AiAnalysisView aiAnalysis,
        List<MediaMetadataView> evidenceFiles, // Gathered from Module 5 Port
        String reviewStatus,
        String lockedByAgentName
) {}