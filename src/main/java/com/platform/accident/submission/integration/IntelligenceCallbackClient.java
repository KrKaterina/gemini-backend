package com.platform.accident.submission.integration;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;


/**
 * Interface implemented by the Submission Module for the Intelligence Module to call.
 */
public interface IntelligenceCallbackClient {
    void onAnalysisComplete(String caseId, AiIntelligenceResult result);
    void onAnalysisFailure(String caseId, String errorCode);
}
