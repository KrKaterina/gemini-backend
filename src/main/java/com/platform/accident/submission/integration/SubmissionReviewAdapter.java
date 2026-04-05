package com.platform.accident.submission.integration;

import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.accident.review.integration.ReportViewerClient;
import com.platform.integration.review.AccidentSnapshotView;
import com.platform.integration.review.AiAnalysisView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubmissionReviewAdapter implements ReportViewerClient {
    private final AccidentRepository repository;

    @Override
//    public Optional<AccidentSnapshotView> getRawData(String caseId) {
//        return repository.findByCaseId(caseId).map(report -> new AccidentSnapshotView(
//                report.getCaseId(),
//                report.getRawDescription(),
//                report.getReporterId(),
//                report.getLocation().lat(),
//                report.getLocation().lng(),
//                report.getContextData() != null ? report.getContextData().weatherCondition() : "N/A",
//                report.getContextData() != null ? report.getContextData().roadType() : "N/A",
//                report.getOccurrenceTime()
//        ));
//    }
    public Optional<AccidentSnapshotView> getRawData(String caseId) {
        return repository.findByCaseId(caseId).map(report -> {

            // Μετατρέπουμε το Map της βάσης πίσω σε AiAnalysisView Record
            var ai = report.getAiAnalysis();
            AiAnalysisView aiView = (ai != null) ? new AiAnalysisView(
                    (String) ai.get("summary"),
                    (String) ai.get("severityLevel"),
                    (java.util.List<String>) ai.get("suggestedNextSteps"),
                    (String) ai.get("detailedReasoning")
            ) : null;

            return new AccidentSnapshotView(
                    report.getCaseId(),
                    report.getRawDescription(),
                    report.getReporterId(),
                    report.getLocation().lat(),
                    report.getLocation().lng(),
                    report.getContextData() != null ? report.getContextData().weatherCondition() : "N/A",
                    report.getContextData() != null ? report.getContextData().roadType() : "N/A",
                    report.getOccurrenceTime()
            );
        });
    }
}
