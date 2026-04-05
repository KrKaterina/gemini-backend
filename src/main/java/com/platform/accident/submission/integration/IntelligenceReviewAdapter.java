package com.platform.accident.submission.integration;


import com.platform.accident.intelligence.repository.AiLogRepository;
import com.platform.accident.review.integration.AiInsightClient;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.integration.review.AiAnalysisView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Υλοποίηση του interface AiInsightClient (από το Module 4).
 * Αυτός ο Adapter επιτρέπει στο Review Module να βλέπει τα αποτελέσματα της τεχνητής νοημοσύνης
 * χωρίς να ξέρει την εσωτερική δομή της βάσης δεδομένων του AI Module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceReviewAdapter implements AiInsightClient {

    private final AccidentRepository repository;

    @Override
    public Optional<AiAnalysisView> getAnalysisResult(String caseId) {
        // Διαβάζουμε το Report για να δούμε τι αποθήκευσε το Gemini ή ο πράκτορας
        return repository.findByCaseId(caseId).map(report -> {
            var ai = report.getAiAnalysis();
            if (ai == null) return null;

            return new AiAnalysisView(
                    (String) ai.getOrDefault("summary", "No summary"),
                    (String) ai.getOrDefault("severityLevel", "LOW"),
                    (List<String>) ai.getOrDefault("suggestedNextSteps", List.of()),
                    (String) ai.getOrDefault("detailedReasoning", "No detailed reasoning available")
            );
        });
    }
}