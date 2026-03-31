package com.platform.accident.intelligence.service;


import com.platform.accident.intelligence.client.factory.AiProviderFactory;
import com.platform.accident.submission.integration.*;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.repository.AiAnalysisLog;
import com.platform.accident.intelligence.repository.AiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;


@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceOrchestrator implements IntelligenceOrchestrationClient {

    private final ReportViewerClient reportViewer;
    private final IntelligenceCallbackClient callbackClient;
    private final AiLogRepository logRepository;
    private final AiProviderFactory providerFactory;

    @Value("${ai.provider}")
    private String providerName;

    /**
     * Public entry point called by Submission Module.
     * Implements Asynchronous processing requirement.
     */
    @Async
    @Override
    public void processAiAnalysis(String caseId) {
        try {
            // 1. Παίρνουμε το εμπλουτισμένο πακέτο
            AnalysisSourceData sourceData = reportViewer.getSourceDataForAnalysis(caseId)
                    .orElseThrow(() -> new IllegalStateException("Source not found"));

            // 2. Φτιάχνουμε το κείμενο
            String prompt = constructConsolidatedPrompt(sourceData);

            // 3. Επιλέγουμε τον Provider
            AiModelProvider aiModelProvider = providerFactory.getProvider(providerName);

            // 4. Στέλνουμε ΤΑ ΠΑΝΤΑ
            Instant startTime = Instant.now();
            AiIntelligenceResult aiResult = aiModelProvider.analyzeIncident(prompt);

            // 5. Καταγραφή & CallbacksaveAnalysisLog(caseId, aiModelProvider.getProviderName(), startTime);
            callbackClient.onAnalysisComplete(caseId, aiResult);
            saveAnalysisLog(caseId, aiModelProvider.getProviderName(), startTime, prompt);
            log.info("AI RESULT for case {} -> severity={}", caseId, aiResult.severityLevel());

        } catch (Exception e) {
            log.error("Analysis failed: {}", e.getMessage());
            callbackClient.onAnalysisFailure(caseId, "AI_ERROR");
        }
    }

    private String constructConsolidatedPrompt(AnalysisSourceData data) {
        // Φτιάχνουμε ΑΥΤΟΜΑΤΑ το string
        String time = (data.occurrenceTime() != null) ? data.occurrenceTime().toString() : "Unknown Date";

        return String.format(
                "Incident occurred on %s. Weather: %s. Road Type: %s. " +
                        "Location: Lat %.4f, Lng %.4f. Description provided: %s. " +
                        "Please analyze this along with the attached media.",
                time,
                data.weatherCondition(),
                data.roadType(),
                data.lat(),
                data.lng(),
                data.rawDescription()
        );
    }

    private void saveAnalysisLog(String caseId, String model, Instant start, String prompt) {
        AiAnalysisLog auditLog = new AiAnalysisLog();
        auditLog.setCaseId(caseId);
        auditLog.setModelId(model);
        auditLog.setProcessedAt(Instant.now());
        auditLog.setDurationMs(Instant.now().toEpochMilli() - start.toEpochMilli());
        auditLog.setSentPrompt(prompt);
        logRepository.save(auditLog);
    }
}