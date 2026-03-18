//package com.platform.accident.enrichment;
//
//import com.platform.accident.enrichment.service.EnrichmentOrchestrator;
//import com.platform.accident.submission.domain.Location;
//import com.platform.accident.submission.integration.EnrichmentResponse;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//public class EnrichmentIntegrationTest {
//
//    @Autowired
//    private EnrichmentOrchestrator orchestrator;
//
//    @Test
//    void testWeatherAndRoadFetch() {
//        // 1. Δημιουργούμε ένα test location
//        Location loc = new Location(40.6401, 22.9444, "Thessaloniki, Greece");
//
//        // 2. Καλούμε τον orchestrator
//        EnrichmentResponse result = orchestrator.enrichAccidentContext("TEST_CASE_001", loc);
//
//        // 3. Εκτυπώνουμε τα δεδομένα για να δούμε τι επιστρέφει
//        System.out.println("Enrichment Result: " + result);
//
//        // 4. Ελέγχουμε αν παίρνουμε δεδομένα καιρού και δρόμου
//        assertNotNull(result.get("weather"), "Weather data should not be null");
//        assertNotNull(result.get("road"), "Road data should not be null");
//        assertFalse((Boolean) result.getOrDefault("contextUnavailable", true), "Context should be available");
//    }
//}