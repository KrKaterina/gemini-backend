package com.platform.accident.intelligence.service;


import com.platform.accident.submission.integration.*;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.repository.AiAnalysisLog;
import com.platform.accident.intelligence.repository.AiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceOrchestrator implements IntelligenceOrchestrationClient {

    private final ReportViewerClient reportViewer;
    private final IntelligenceCallbackClient callbackClient;
    private final AiModelProvider aiModelProvider;
    private final AiLogRepository logRepository;

    /**
     * Public entry point called by Submission Module.
     * Implements Asynchronous processing requirement.
     */
    @Async
    @Override
    public void processAiAnalysis(String caseId) {
        log.info("Starting AI analysis orchestration for Case ID: {}", caseId);

        try {
            // 1. Data Gathering (Cross-Module View)
            AnalysisSourceData sourceData = reportViewer.getSourceDataForAnalysis(caseId)
                    .orElseThrow(() -> new IllegalStateException("Source data not found for " + caseId));

            // 2. Prompt Engineering (Internal Logic)
            String prompt = constructConsolidatedPrompt(sourceData);

            // 3. AI Inference (External Dependency)
            Instant startTime = Instant.now();
            AiIntelligenceResult aiResult = aiModelProvider.analyzeIncident(prompt);

            // 4. Persistence (Local Module Audit Log)
            saveAnalysisLog(caseId, aiModelProvider.getProviderName(), startTime);

            // 5. Completion Callback
            callbackClient.onAnalysisComplete(caseId, aiResult);

        } catch (Exception e) {
            log.error("AI Analysis failed for Case {}: {}", caseId, e.getMessage());
            callbackClient.onAnalysisFailure(caseId, "AI_PROCESSING_ERROR");
        }
    }

    private String constructConsolidatedPrompt(AnalysisSourceData data) {
        return String.format(
                "Analyze this road accident. Context: Weather is %s, Road is %s. " +
                        "Coordinates: [%f, %f]. Incident Description: %s",
                data.weatherCondition(), data.roadType(),
                data.lat(), data.lng(), data.rawDescription()
        );
    }

    private void saveAnalysisLog(String caseId, String model, Instant start) {
        AiAnalysisLog auditLog = new AiAnalysisLog();
        auditLog.setCaseId(caseId);
        auditLog.setModelId(model);
        auditLog.setProcessedAt(Instant.now());
        auditLog.setDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
        logRepository.save(auditLog);
    }
}