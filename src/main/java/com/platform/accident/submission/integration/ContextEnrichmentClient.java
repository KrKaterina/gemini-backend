package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import java.util.List;
import java.util.Map;

public interface ContextEnrichmentClient {
    // FIX: Typed DTO return instead of Map<String, Object>
    EnrichmentResponse enrichAccidentContext(String caseId, Location location);
}