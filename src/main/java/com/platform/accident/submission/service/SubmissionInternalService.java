package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.submission.repository.AccidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmissionInternalService {
    private final AccidentRepository repository;

    @Transactional
    public void updateFinalStatus(String caseId, AccidentStatus status, String agentId, String severity) {
        repository.findByCaseId(caseId).ifPresent(report -> {
            report.setStatus(status);
            if (report.getAiAnalysis() != null) {
                report.getAiAnalysis().put("verifiedBy", agentId);
                report.getAiAnalysis().put("finalSeverity", severity);
            }
            repository.save(report);
        });
    }
}