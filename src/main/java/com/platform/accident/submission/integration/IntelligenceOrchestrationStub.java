//package com.platform.accident.submission.integration;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service // This will now serve both profiles unless a Prod version is created
//@org.springframework.context.annotation.Profile("dev")
//public class IntelligenceOrchestrationStub implements IntelligenceOrchestrationClient {
//    @Override
//    public void processAiAnalysis(String caseId) {
//        log.info("[AI Module] Initiating LLM extraction and severity analysis for {}", caseId);
//    }
//}