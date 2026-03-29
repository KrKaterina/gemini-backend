package com.platform.accident.submission.integration;

import java.util.Optional;

public interface UnifiedCaseViewerClient {
    Optional<CaseFileView> getCompleteCaseFile(String caseId);
}