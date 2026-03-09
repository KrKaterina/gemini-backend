package com.platform.accident.submission.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class IntelligenceOrchestrationStub implements IntelligenceOrchestrationClient {
    @Override
    public void processAiAnalysis(String caseId) {
        log.info("[AI Module] Initiating LLM extraction and severity analysis for {}", caseId);
        // In a real scenario, this might trigger an @Async method or push to an internal event bus
    }
}