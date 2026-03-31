package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.*;
import com.platform.accident.submission.exception.AccidentNotFoundException;
import com.platform.accident.submission.integration.*;
import com.platform.accident.submission.repository.AccidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final AccidentRepository repository;
    private final AccidentValidator validator;
    private final MediaAssetClient mediaClient;
    private final ContextEnrichmentClient contextClient;
    private final IntelligenceOrchestrationClient intelligenceClient;

    @Transactional
    public AccidentReport submitAccident(AccidentReportInput input, String userId) {
        validator.validateInput(input);

        String caseId = generateCaseId();

        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .reporterId(userId)
                .status(AccidentStatus.RECEIVED)
                .createdAt(Instant.now())
                .location(input.location())
                .occurrenceTime(input.occurrenceTime())
                .rawDescription(input.description())
                .assetIds(input.assetIds())
                .build();

        repository.save(report);
        triggerBackgroundProcesses(caseId, input.location());
        return report;
    }

    private void triggerBackgroundProcesses(String caseId, Location loc) {
        CompletableFuture.runAsync(() -> {
            try {
                // FIX: Immediately update status to show work in progress
                updateReportStatus(caseId, AccidentStatus.ENRICHING);

                // FIX: Interface returns neutral EnrichmentResponse (No illegal casts to enrichment.domain)
                EnrichmentResponse response = contextClient.enrichAccidentContext(caseId, loc);

                // FIX: Atomic retrieval and update for enrichment data
                repository.findByCaseId(caseId).ifPresent(report -> {

                    EnrichedContext context = EnrichedContext.builder()
                            .weatherCondition(response.weatherCondition())
                            .temperatureCelsius(response.temperature())
                            .roadType(response.roadType())
                            .neighborhood(response.streetName())
                            .daylight(true) // Derived or hardcoded for now
                            .build();

                    report.setContextData(context);
                    report.setStatus(AccidentStatus.ANALYZING); // Proceed to AI state
                    repository.save(report);

                    log.info("Accident report {} successfully enriched and moved to AI analysis.", caseId);

                    // Trigger subsequent AI module
                    intelligenceClient.processAiAnalysis(caseId);
                });

            } catch (Exception e) {
                log.error("Critical failure in background workflow for case {}: {}", caseId, e.getMessage());
            }
        });
    }

    private void updateReportStatus(String caseId, AccidentStatus status) {
        repository.findByCaseId(caseId).ifPresent(report -> {
            report.setStatus(status);
            repository.save(report);
        });
    }

    private String generateCaseId() {
        return "ACC-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }

    public AccidentReport getReport(String caseId) {
        return repository.findByCaseId(caseId).orElseThrow(() -> new AccidentNotFoundException(caseId));
    }
}