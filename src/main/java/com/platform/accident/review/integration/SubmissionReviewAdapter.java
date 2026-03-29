package com.platform.accident.review.integration;

//import com.platform.accident.submission.domain.AccidentStatus;
//import com.platform.accident.submission.integration.ReviewCallbackPort;
//import com.platform.accident.submission.repository.AccidentRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
///**
// * Adapter that provides the feedback loop to the Submission module.
// * Real implementation would update the Submission aggregate status.
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class SubmissionReviewAdapter implements ReviewCallbackPort {
//
//    private final AccidentRepository accidentRepository;
//
//    @Override
//    public void markCaseAsVerified(String caseId, String finalSeverity, String agentId) {
//        accidentRepository.findByCaseId(caseId).ifPresent(report -> {
//            report.setStatus(AccidentStatus.PROCESSED);
//            // We ensure the severity reflects the human decision
//            if (report.getAiAnalysis() != null) {
//                report.getAiAnalysis().put("severity", finalSeverity);
//                report.getAiAnalysis().put("verifiedBy", agentId);
//            }
//            accidentRepository.save(report);
//        });
//    }
//
//    @Override
//    public void markCaseAsRejected(String caseId, String reason, String agentId) {
//        accidentRepository.findByCaseId(caseId).ifPresent(report -> {
//            report.setStatus(AccidentStatus.PROCESSED); // Or a specific REJECTED status if added to Enum
//            report.getWorkflow().put("rejectionReason", reason);
//            report.getWorkflow().put("rejectedBy", agentId);
//            accidentRepository.save(report);
//        });
//    }
//}

import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.integration.ReviewCallbackPort;
import com.platform.accident.submission.service.SubmissionInternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubmissionReviewAdapter implements ReviewCallbackPort {

    private final SubmissionInternalService submissionService;

    @Override
    public void markCaseAsVerified(String caseId, String finalSeverity, String agentId) {
        submissionService.updateFinalStatus(caseId, AccidentStatus.PROCESSED, agentId, finalSeverity);
    }

    @Override
    public void markCaseAsRejected(String caseId, String reason, String agentId) {
        submissionService.updateFinalStatus(caseId, AccidentStatus.REJECTED, agentId, "N/A");
    }
}