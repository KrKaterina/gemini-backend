package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.*;
import com.platform.accident.submission.exception.AccidentNotFoundException;
import com.platform.accident.submission.integration.*;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.media.MediaAssetClient;
import com.platform.integration.policy.EligibilityStatus;
import com.platform.integration.policy.PolicyPort;
import com.platform.policy.exception.IneligibleReportingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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

    private final IdentityClient identityClient;

    private final PolicyPort policyPort; // New Bridge to Module 7


    @Transactional
    public AccidentReport submitAccident(AccidentReportInput input, String userId) {
        // Audit before logic starts
        identityClient.logSecurityEvent(userId, "REPORT_SUBMISSION_START", "Case in progress");

        // NEW ENFORCEMENT: Point-of-entry block
        EligibilityStatus coverage = policyPort.checkEligibility(userId, input.occurrenceTime());

        if (!coverage.eligible()) {
            // Failure Trace
            identityClient.logSecurityEvent(userId, "REJECTED_REPORT_INELIGIBLE", coverage.reasonCode());
            throw new IneligibleReportingException("Accident date not covered by an active policy: " + coverage.reasonCode());
        }

        // Proceed to generate report only if covered
        String caseId = generateCaseId();

        validator.validateInput(input);

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

        mediaClient.linkAssetsToCase(caseId, input.assetIds());

        // ASYNC PROPAGATION: Pass the context-derived userId explicitly
        // to the background thread to avoid context loss.
        triggerBackgroundProcesses(caseId, input.location(), userId);
        return report;
    }

    private void triggerBackgroundProcesses(String caseId, Location loc, String traceUserId) {
        CompletableFuture.runAsync(() -> {
            identityClient.logSecurityEvent(traceUserId, "AUTOMATED_PROCESSING_START", caseId);

            try {
                updateReportStatus(caseId, AccidentStatus.ENRICHING);

                EnrichmentResponse response = contextClient.enrichAccidentContext(caseId, loc);

                repository.findByCaseId(caseId).ifPresent(report -> {


                    Integer parsedSpeed = parseSpeed(response.speedLimit());

                    EnrichedContext context = EnrichedContext.fromResponse(response, parsedSpeed);

                    report.setContextData(context);
                    report.setStatus(AccidentStatus.ANALYZING);
                    repository.save(report);

                    log.info("Accident report {} successfully enriched and moved to AI analysis.", caseId);

                    intelligenceClient.processAiAnalysis(caseId, traceUserId);
                });

            } catch (Exception e) {
                log.error("Critical failure in background workflow for case {}: {}", caseId, e.getMessage());
            }
        });
    }


    private Integer parseSpeed(String speed) {
        if (speed == null || speed.isBlank()) return 50;
        try {
            String sanitized = speed.replaceAll("[^0-9]", "");
            return sanitized.isEmpty() ? 50 : Integer.parseInt(sanitized);
        } catch (Exception e) {
            return 50;
        }
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

    public List<AccidentReport> getReportsByReporter(String reporterId) {
        return repository.findByReporterId(reporterId);
    }
}