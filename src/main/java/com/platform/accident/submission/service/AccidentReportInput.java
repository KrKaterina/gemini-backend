package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.Location;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

/**
 * Internal record used by the Service layer to decouple API DTOs
 * from business logic.
 */
public record AccidentReportInput(
        Instant occurrenceTime,
        Location location,
        String description,
        List<String> assetIds
) {}