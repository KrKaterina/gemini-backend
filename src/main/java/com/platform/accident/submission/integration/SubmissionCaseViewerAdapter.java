package com.platform.accident.submission.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.submission.repository.AccidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubmissionCaseViewerAdapter implements UnifiedCaseViewerClient {

    private final AccidentRepository repository;
    private final ObjectMapper objectMapper; // Use the configured AI-resilient mapper

    @Override
//    public Optional<CaseFileView> getCompleteCaseFile(String caseId) {
//        return repository.findByCaseId(caseId).map(report -> new CaseFileView(
//                report.getCaseId(),
//                report.getRawDescription(),
//                report.getLocation(),
//                report.getAiAnalysis() != null ? (String) report.getAiAnalysis().get("summary") : null,
//                report.getAiAnalysis() != null ? (String) report.getAiAnalysis().get("severity") : null,
//                report.getContextData() != null ? report.getContextData().weatherCondition() : "N/A",
//                report.getContextData() != null ? report.getContextData().roadType() : "N/A",
//                report.getContextData() == null // enrichmentUnavailable if contextData is null
//        ));
//    }
    public Optional<CaseFileView> getCompleteCaseFile(String caseId) {
        return repository.findByCaseId(caseId).map(report -> {

            // Safe conversion of Map to Intelligence Record
            AiIntelligenceResult ai = null;
            if (report.getAiAnalysis() != null) {
                ai = objectMapper.convertValue(report.getAiAnalysis(), AiIntelligenceResult.class);
            }

            return new CaseFileView(
                    report.getCaseId(),
                    report.getRawDescription(),
                    report.getLocation(),
                    ai != null ? ai.summary() : null,
                    ai != null ? ai.severityLevel() : null,
                    report.getContextData() != null ? report.getContextData().weatherCondition() : "N/A",
                    report.getContextData() != null ? report.getContextData().roadType() : "N/A",
                    report.getContextData() == null
            );
        });
    }
}