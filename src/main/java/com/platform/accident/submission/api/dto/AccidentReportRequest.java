package com.platform.accident.submission.api.dto;

import com.platform.accident.submission.domain.Location;
import java.time.Instant;
import java.util.List;

public record AccidentReportRequest(
        Instant occurrenceTime,
        Location location,
        String description,
        List<String> assetIds
) {}
