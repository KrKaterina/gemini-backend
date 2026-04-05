package com.platform.accident.review.integration;

import java.util.Map;

/**
 * Port to push finalized review data and status to the Submission Module.
 */
public interface ReportLifecycleClient {
    void finalizeReport(String caseId, Map<String, Object> finalData);
    void updateStatus(String caseId, String status);
}