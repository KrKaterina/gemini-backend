package com.platform.accident.submission.integration;

public interface ReviewCallbackPort {
    void markCaseAsVerified(String caseId, String finalSeverity, String agentId);
    void markCaseAsRejected(String caseId, String reason, String agentId);
}
