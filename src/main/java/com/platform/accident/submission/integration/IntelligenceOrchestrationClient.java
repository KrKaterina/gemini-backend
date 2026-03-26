package com.platform.accident.submission.integration;

import org.springframework.context.annotation.Profile;

public interface IntelligenceOrchestrationClient {
    void processAiAnalysis(String caseId);
}

