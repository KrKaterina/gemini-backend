package com.platform.accident.submission.integration;

import org.springframework.context.annotation.Profile;

public interface IntelligenceOrchestrationClient {
    // We update the method to accept the traceId for the audit log
    void processAiAnalysis(String caseId, String traceUserId);}

