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
import java.util.List;
import java.util.Map;
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

//    @Transactional
//    public AccidentReport submitAccident(AccidentReportInput input, String userId) {
//        validator.validateInput(input);
//
//        String caseId = generateCaseId();
//
//        AccidentReport report = AccidentReport.builder()
//                .caseId(caseId)
//                .reporterId(userId)
//                .status(AccidentStatus.RECEIVED)
//                .createdAt(Instant.now())
//                .occurrenceTime(input.occurrenceTime())
//                .location(input.location())
//                .rawDescription(input.description())
//                .assets(input.assetIds().stream()
//                        .map(id -> new AssetReference(id, "UNSPECIFIED"))
//                        .toList())
//                .build();
//
//        // 1. Persist Initial Draft
//        AccidentReport saved = repository.save(report);
//
//        // 2. Link Media Assets
//        mediaClient.linkAssetsToCase(caseId, input.assetIds());
//
//        // 3. Enrich with Environmental Context (Weather/Maps)
//        try {
//            saved.setStatus(AccidentStatus.ENRICHING);
//            var enrichedData = contextClient.enrichAccidentContext(caseId, input.location());
//            saved.setContextData(enrichedData);
//        } catch (Exception e) {
//            log.error("Context enrichment failed for {}", caseId, e);
//        }
//
//        // 4. Hand off to AI for background analysis
//        saved.setStatus(AccidentStatus.ANALYZING);
//        repository.save(saved);
//
//        intelligenceClient.processAiAnalysis(caseId);
//
//        return saved;
//    }

    @Transactional
    public AccidentReport submitAccident(AccidentReportInput input, String userId) {
        validator.validateInput(input);
        validator.verifyAssetOwnership(userId, input.assetIds()); // Security Check

        String caseId = generateCaseId();

        // Logical Asset Enrichment instead of "UNSPECIFIED"
        List<AssetReference> categorizedAssets = input.assetIds().stream()
                .map(id -> new AssetReference(id, id.contains("aud") ? AssetType.AUDIO_TESTIMONY.name() : AssetType.VEHICLE_PHOTO.name()))
                .toList();

        AccidentReport report = AccidentReport.builder()
                .caseId(caseId)
                .reporterId(userId)
                .status(AccidentStatus.RECEIVED)
                .createdAt(Instant.now())
                .location(input.location())
                .assets(categorizedAssets)
                .build();

        repository.save(report);

        // ASYNC ORCHESTRATION: The interfaces themselves should now be @Async
        // or wrapped in a TaskExecutor to prevent blocking the controller
        triggerBackgroundProcesses(caseId, input.location());

        return report;
    }

    // Triggered after primary DB save
//    private void triggerBackgroundProcesses(String caseId, Location loc) {
//        CompletableFuture.runAsync(() -> {
//            try {
//                // Calls stubs for Context & AI
//                contextClient.enrichAccidentContext(caseId, loc);
//                intelligenceClient.processAiAnalysis(caseId);
//            } catch (Exception e) {
//                log.error("Background processing failed for case: {}", caseId, e);
//            }
//        });
//    }
    private void triggerBackgroundProcesses(String caseId, Location loc) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting background enrichment for case: {}", caseId);

                // 1. Παίρνουμε τα δεδομένα από τον Orchestrator
                Map<String, Object> result = contextClient.enrichAccidentContext(caseId, loc);

                // 2. Βρίσκουμε το ατύχημα στη βάση
                repository.findByCaseId(caseId).ifPresent(report -> {

                    // 3. Μετατρέπουμε το Map στο Record EnrichedContext
                    var weather = (com.platform.accident.enrichment.domain.WeatherMetrics) result.get("weather");
                    var road = (com.platform.accident.enrichment.domain.RoadGeometry) result.get("road");

                    EnrichedContext context = EnrichedContext.builder()
                            .weatherCondition(weather != null ? weather.condition() : "UNKNOWN")
                            .temperatureCelsius(weather != null ? weather.temperature() : 0.0)
                            .roadType(road != null ? road.roadType() : "unknown")
                            .neighborhood(road != null ? road.streetName() : "unknown")
                            .daylight(true) // Ή υπολόγισέ το αν θέλεις
                            .build();

                    // 4. Ενημερώνουμε το Report
                    report.setContextData(context);
                    report.setStatus(AccidentStatus.ENRICHING); // Ή ENRICHED αν το προσθέσεις στο Enum

                    // 5. ΣΩΖΟΥΜΕ ΣΤΗ ΒΑΣΗ (Το πιο σημαντικό βήμα που έλειπε)
                    repository.save(report);
                    log.info("Successfully updated accident report {} with road and weather data.", caseId);
                });

                intelligenceClient.processAiAnalysis(caseId);
            } catch (Exception e) {
                log.error("Background processing failed for case: {}", caseId, e);
            }
        });
    }

    public AccidentReport getReport(String caseId) {
        return repository.findByCaseId(caseId)
                .orElseThrow(() -> new AccidentNotFoundException(caseId));
    }

    private String generateCaseId() {
        return "ACC-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
    }
}