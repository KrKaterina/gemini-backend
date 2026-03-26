package com.platform.accident.submission.service;

import com.platform.accident.submission.api.dto.AccidentReportRequest;
import org.springframework.stereotype.Component;

@Component
public class AccidentMapper {
    public AccidentReportInput toInput(AccidentReportRequest request) {
        return new AccidentReportInput(
                request.occurrenceTime(),
                request.location(),
                request.description(),
                request.assetIds(),
                java.util.List.of(),
                null
        );
    }
}
