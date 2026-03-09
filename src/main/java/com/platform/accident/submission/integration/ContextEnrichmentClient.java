package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import java.util.List;
import java.util.Map;

public interface ContextEnrichmentClient {
    Map<String, Object> enrichAccidentContext(String caseId, Location location);
}
