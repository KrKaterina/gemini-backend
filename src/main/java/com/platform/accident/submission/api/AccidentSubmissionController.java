package com.platform.accident.submission.api;


import com.platform.accident.submission.api.dto.*;
import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accidents")
@RequiredArgsConstructor
public class AccidentSubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ResponseEntity<AccidentSubmissionResponse> submitReport(
            @RequestBody AccidentReportRequest request,
            @RequestHeader("X-User-Id") String userId) {

        // Map DTO to internal service record
        var input = new com.platform.accident.submission.service.AccidentReportInput(
                request.occurrenceTime(),
                request.location(),
                request.description(),
                request.assetIds()
        );

        AccidentReport report = submissionService.submitAccident(input, userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AccidentSubmissionResponse(
                report.getCaseId(),
                report.getStatus().name(),
                report.getCreatedAt()
        ));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<AccidentReport> getReport(@PathVariable String caseId) {
        return ResponseEntity.ok(submissionService.getReport(caseId));
    }
}