package com.platform.accident.intelligence.service;


import com.platform.accident.intelligence.client.factory.AiProviderFactory;
import com.platform.accident.submission.integration.*;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.repository.AiAnalysisLog;
import com.platform.accident.intelligence.repository.AiLogRepository;
import com.platform.integration.identity.IdentityClient;
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
    private final IdentityClient identityClient;

    @Value("${ai.provider}")
    private String providerName;

    /**
     * Public entry point called by Submission Module.
     * Implements Asynchronous processing requirement.
     */
    @Async("intelligenceTaskExecutor")
    @Override
//    public void processAiAnalysis(String caseId) {
//        try {
//            // 1. Παίρνουμε το εμπλουτισμένο πακέτο
//            AnalysisSourceData sourceData = reportViewer.getSourceDataForAnalysis(caseId)
//                    .orElseThrow(() -> new IllegalStateException("Source not found"));
//
//            // 2. Φτιάχνουμε το κείμενο
//            String prompt = constructConsolidatedPrompt(sourceData);
//
//            // 3. Επιλέγουμε τον Provider
//            AiModelProvider aiModelProvider = providerFactory.getProvider(providerName);
//
//            // 4. Στέλνουμε ΤΑ ΠΑΝΤΑ
//            Instant startTime = Instant.now();
//            AiIntelligenceResult aiResult = aiModelProvider.analyzeIncident(prompt);
//
//            // 5. Καταγραφή & CallbacksaveAnalysisLog(caseId, aiModelProvider.getProviderName(), startTime);
//            callbackClient.onAnalysisComplete(caseId, aiResult);
//            saveAnalysisLog(caseId, aiModelProvider.getProviderName(), startTime, prompt);
//            log.info("AI RESULT for case {} -> severity={}", caseId, aiResult.severityLevel());
//
//        } catch (Exception e) {
//            log.error("Analysis failed: {}", e.getMessage());
//            callbackClient.onAnalysisFailure(caseId, "AI_ERROR");
//        }
//    }
    public void processAiAnalysis(String caseId, String traceUserId) {
        identityClient.logSecurityEvent(traceUserId, "AUTOMATED_AI_PROCESSING", "Started analysis for " + caseId);

        Instant startTime = Instant.now();
        try {
            AnalysisSourceData sourceData = reportViewer.getSourceDataForAnalysis(caseId)
                    .orElseThrow(() -> new IllegalStateException("Source not found for " + caseId));

            // Use Component Names: sourceData.occurrenceTime() instead of getOccurrenceTime()
            String expertPrompt = String.format(
                    "Reconstruct accident at %s. Context: %s on %s. Neighborhood: %s. Daylight: %s. Description: %s",
                    sourceData.occurrenceTime(), // 8
                    sourceData.weatherCondition(), // 2
                    sourceData.roadType(), // 3
                    sourceData.neighborhood(), // 4
                    sourceData.isDaylight() ? "Yes" : "No", // 5
                    sourceData.rawDescription() // 1
            );

            AiModelProvider model = providerFactory.getProvider(providerName);

            // CALL WITH TWO PARAMETERS (Prompt and the List from record)
            AiIntelligenceResult result = model.analyzeIncident(
                    expertPrompt,
                    sourceData.assetIds() // 10
            );

            saveAnalysisLog(caseId, model.getProviderName(), startTime, expertPrompt);
            callbackClient.onAnalysisComplete(caseId, result);

            log.info("Successfully analyzed case {}. AI Severity identified as: {}", caseId, result.severityLevel());
        } catch (Exception e) {
            log.error("AI Analysis critical failure for case {}: {}", caseId, e.getMessage());
            callbackClient.onAnalysisFailure(caseId, "AI_PROVIDER_ERROR");

            identityClient.logSecurityEvent(traceUserId, "AI_FAILURE", "Analysis failed: " + e.getMessage());
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
        auditLog.setSuccessful(true);
        logRepository.save(auditLog);
    }
}