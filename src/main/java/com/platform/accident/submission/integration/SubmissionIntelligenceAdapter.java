package com.platform.accident.submission.integration;


import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionIntelligenceAdapter implements ReportViewerClient, IntelligenceCallbackClient {

    private final AccidentRepository repository;

    /**
     * READ: Implementation of ReportViewerClient.
     * Maps the Submission domain to the neutral AnalysisSourceData DTO.
     */
    @Override
    public Optional<AnalysisSourceData> getSourceDataForAnalysis(String caseId) {
        return repository.findByCaseId(caseId)
                .map(report -> new AnalysisSourceData(
                        report.getRawDescription(),
                        report.getContextData() != null ? report.getContextData().weatherCondition() : "Unknown",
                        report.getContextData() != null ? report.getContextData().roadType() : "Unknown",
                        report.getLocation().lat(),
                        report.getLocation().lng(),
                        report.getOccurrenceTime(),
                        report.getImages(),
                        report.getAudioRecording()
                ));
    }
    /**
     * WRITE: Implementation of IntelligenceCallbackClient.
     * Persists the AI results back into the main Accident aggregate.
     */
    @Override
    //δουλευει
//    public void onAnalysisComplete(String caseId, AiIntelligenceResult aiResult) {
//        repository.findByCaseId(caseId).ifPresent(report -> {
//
//            // Τα πεδία μπαίνουν στη βάση αφού το AI απάντησε
//            java.util.Map<String, Object> finalData = new java.util.HashMap<>();
//
//            finalData.put("severity", aiResult.severityLevel()); // Low, Medium, High, Fatal
//            finalData.put("detailedReconstruction", aiResult.rawAiOutput());
//            finalData.put("analysisDate", java.time.Instant.now());
//
//            report.setAiAnalysis(finalData);
//
//            // Αν το AI έβγαλε FATAL ή HIGH, το status στη βάση γίνεται άμεσο PENDING_REVIEW
//            if ("FATAL".equals(aiResult.severityLevel()) || "HIGH".equals(aiResult.severityLevel())) {
//                report.setStatus(AccidentStatus.PENDING_REVIEW); // status για επείγον
//            } else {
//                report.setStatus(AccidentStatus.PROCESSED);
//            }
//
//            repository.save(report);
//            log.info("Analysis persisted in MongoDB with severity: {}", aiResult.severityLevel());
//        });
//    }
    public void onAnalysisComplete(String caseId, AiIntelligenceResult aiResult) {
        repository.findByCaseId(caseId).ifPresent(report -> {
            java.util.Map<String, Object> finalData = new java.util.HashMap<>();

            // FIX: Use keys that match the AiIntelligenceResult record fields
            finalData.put("summary", aiResult.rawAiOutput()); // Maps to summary()
            finalData.put("severityLevel", aiResult.severityLevel()); // Maps to severityLevel()
            finalData.put("entities", aiResult.entities());
            finalData.put("suggestedNextSteps", aiResult.suggestedNextSteps());
            finalData.put("analysisDate", java.time.Instant.now());

            report.setAiAnalysis(finalData);

            if ("FATAL".equals(aiResult.severityLevel()) || "HIGH".equals(aiResult.severityLevel())) {
                report.setStatus(AccidentStatus.PENDING_REVIEW);
            } else {
                report.setStatus(AccidentStatus.PROCESSED);
            }

            repository.save(report);
        });
    }

    @Override
    public void onAnalysisFailure(String caseId, String errorCode) {
        log.error("AI Analysis failed for case {}. Error code: {}", caseId, errorCode);
        repository.findByCaseId(caseId).ifPresent(report -> {
            report.setStatus(AccidentStatus.PENDING_REVIEW); // Allow agent to process manually
            repository.save(report);
        });
    }
}