package com.platform.accident.submission.integration;

import com.platform.accident.review.domain.ReviewCase;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.service.ReviewService;
import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionIntelligenceAdapter implements ReportViewerClient, IntelligenceCallbackClient {

    private final AccidentRepository repository;
    private final ReviewService reviewService; // BRIDGING: Calling Module 4 directly via internal interface

    /**
     * READ: Implementation of ReportViewerClient.
     * Maps the Submission domain to the neutral AnalysisSourceData DTO.
     */
    @Override
    public Optional<AnalysisSourceData> getSourceDataForAnalysis(String caseId) {
        return repository.findByCaseId(caseId).map(report -> {
            var ctx = report.getContextData();
            return new AnalysisSourceData(
                    report.getRawDescription(),
                    ctx != null ? ctx.weatherCondition() : "UNKNOWN",
                    ctx != null ? ctx.roadType() : "UNKNOWN",
                    ctx != null ? ctx.neighborhood() : "Unknown Neighborhood", // Fulfilled
                    ctx != null && ctx.daylight(),                             // Fulfilled (Boolean)
                    report.getLocation().lat(),
                    report.getLocation().lng(),
                    report.getOccurrenceTime(),
                    report.getCaseId(),
                    report.getAssetIds() != null ? report.getAssetIds() : java.util.List.of() // 10
            );
        });
    }

    /**
     * WRITE: Implementation of IntelligenceCallbackClient.
     * Persists the AI results back into the main Accident aggregate.
     */
    @Override
    public void onAnalysisComplete(String caseId, AiIntelligenceResult aiResult) {
        repository.findByCaseId(caseId).ifPresent(report -> {
            Map<String, Object> aiMap = new HashMap<>();
            aiMap.put("summary", aiResult.summary());
            aiMap.put("detailedReasoning", aiResult.rawAiOutput());
            aiMap.put("severityLevel", aiResult.severityLevel());
            aiMap.put("entities", aiResult.entities());
            aiMap.put("suggestedNextSteps", aiResult.suggestedNextSteps());

            report.setAiAnalysis(aiMap);
            report.setStatus(AccidentStatus.PENDING_REVIEW);
            repository.save(report);

            // ΚΑΛΟΥΜΕ ΤΟ ΑΛΛΟ MODULE
            reviewService.initializeReviewQueue(caseId);

            log.info("Handover to review module successful for case {}", caseId);
        });
    }


    @Override
    public void onAnalysisFailure(String caseId, String errorCode) {
        log.error("AI failure for case {}. Error: {}", caseId, errorCode);

        repository.findByCaseId(caseId).ifPresent(report -> {
            report.setStatus(AccidentStatus.PENDING_REVIEW);
            repository.save(report);
        });

        // Ακόμα και σε αποτυχία του AI, προωθούμε την υπόθεση για χειροκίνητο έλεγχο
        reviewService.initializeReviewQueue(caseId);
    }

}