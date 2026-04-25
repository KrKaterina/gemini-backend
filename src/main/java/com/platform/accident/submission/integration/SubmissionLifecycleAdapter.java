package com.platform.accident.submission.integration;

import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.accident.submission.domain.AccidentStatus;
import com.platform.accident.review.integration.ReportLifecycleClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Υλοποίηση του interface ReportLifecycleClient (από το Module 4).
 * Αυτός ο Adapter επιτρέπει στο Review Module να ενημερώνει την κεντρική αναφορά
 * ατυχήματος που ανήκει στο Submission Module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionLifecycleAdapter implements ReportLifecycleClient {

    private final AccidentRepository repository;

    @Override
    public void finalizeReport(String caseId, Map<String, Object> finalData) {
        log.info("Applying agent final verification data to report: {}", caseId);

        repository.findByCaseId(caseId).ifPresent(report -> {
            // Ενημερώνουμε τα δεδομένα της ανάλυσης με τις διορθώσεις του ανθρώπου
            if (report.getAiAnalysis() != null) {
                // Συνενώνουμε τις AI αναλύσεις με τις ανθρώπινες διορθώσεις
                report.getAiAnalysis().putAll(finalData);
            } else {
                report.setAiAnalysis(finalData);
            }
            repository.save(report);
        });
    }

    @Override
    public void updateStatus(String caseId, String status) {
        repository.findByCaseId(caseId).ifPresent(report -> {
            log.info("Requesting status change to {} for case {}", status, caseId);
            try {
                // Μετατροπή String σε Enum - Προσοχή να ταυτίζονται!
                report.setStatus(AccidentStatus.valueOf(status));
                repository.save(report);
            } catch (Exception e) {
                log.error("Failed to map status {}. Ensure it exists in AccidentStatus enum.", status);
            }
        });
    }
}
