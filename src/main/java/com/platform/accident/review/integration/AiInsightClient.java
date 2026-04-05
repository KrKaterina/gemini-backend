package com.platform.accident.review.integration;


import com.platform.integration.review.AiAnalysisView;

import java.util.Map;
import java.util.Optional;

/**
 * Port to retrieve AI findings from the Intelligence Module.
 */
public interface AiInsightClient {
    Optional<AiAnalysisView> getAnalysisResult(String caseId);
}
