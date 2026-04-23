package com.platform.accident.submission.api;

import com.platform.accident.submission.api.dto.*;
import com.platform.accident.submission.domain.AccidentReport;
import com.platform.accident.submission.service.AccidentReportInput;
import com.platform.accident.submission.service.SubmissionService;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.identity.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accidents")
@RequiredArgsConstructor
public class AccidentSubmissionController {

    private final SubmissionService submissionService;

//    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<AccidentSubmissionResponse> submitReport(
//            @RequestPart("request") AccidentReportRequest request, // To JSON μέρος
//            @RequestHeader("X-User-Id") String userId) {
//
//        // Map DTO to internal service record
//        var input = new com.platform.accident.submission.service.AccidentReportInput(
//                request.occurrenceTime(),
//                request.location(),
//                request.description(),
//                request.assetIds()
//        );
//
//        AccidentReport report = submissionService.submitAccident(input, userId);
//
//        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AccidentSubmissionResponse(
//                report.getCaseId(),
//                report.getStatus().name(),
//                report.getCreatedAt()
//        ));
//    }

    @GetMapping("/{caseId}")
    public ResponseEntity<AccidentReport> getReport(@PathVariable String caseId) {
        return ResponseEntity.ok(submissionService.getReport(caseId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AccidentSubmissionResponse> submitReport(
            @RequestPart("request") AccidentReportRequest request,
            HttpServletRequest httpRequest) {

        // 1. ΑΣΦΑΛΗΣ ΕΞΑΓΩΓΗ ΤΑΥΤΟΤΗΤΑΣ:
        // Η ταυτότητα πηγάζει από το κρυπτογραφημένο Token και όχι από Headers.
        IdentityContext user = SecurityContext.getRequired(httpRequest);

        // 2. ΜΕΤΑΤΡΟΠΗ ΣΕ INTERNAL INPUT RECORD:
        var input = new com.platform.accident.submission.service.AccidentReportInput(
                request.occurrenceTime(),
                request.location(),
                request.description(),
                request.assetIds()
        );

        // 3. ΕΚΤΕΛΕΣΗ LOGIC:
        // Χρήση του έμπιστου userId από το IdentityContext.
        AccidentReport report = submissionService.submitAccident(input, user.userId());

        // 4. ΠΛΗΡΗΣ ΑΠΟΚΡΙΣΗ (Το σημείο που είχε μείνει ημιτελές):
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new AccidentSubmissionResponse(
                report.getCaseId(),
                report.getStatus().name(),
                report.getCreatedAt()
        ));
    }

    @GetMapping("/my-reports")
    public ResponseEntity<List<AccidentReport>> getMyHistory(HttpServletRequest request) {
        // DERIVE IDENTITY ONLY: We trust the Token-based userId
        var ctx = SecurityContext.getRequired(request);

        List<AccidentReport> reports = submissionService.getReportsByReporter(ctx.userId());

        return ResponseEntity.ok(reports);
    }
}