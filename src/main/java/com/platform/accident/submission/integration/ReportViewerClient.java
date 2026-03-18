package com.platform.accident.submission.integration;

import java.util.Optional;

public interface ReportViewerClient {
    Optional<AnalysisSourceData> getSourceDataForAnalysis(String caseId);
}