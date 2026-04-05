package com.platform.accident.review.integration;

import com.platform.integration.review.AccidentSnapshotView;
import java.util.Optional;

public interface ReportViewerClient {
    // Standardized to use the Snapshot view used by ReviewService
    Optional<AccidentSnapshotView> getRawData(String caseId);
}