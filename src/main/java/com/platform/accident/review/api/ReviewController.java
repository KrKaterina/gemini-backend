package com.platform.accident.review.api;

import com.platform.accident.review.service.ReviewQueryService;
import com.platform.accident.review.service.ReviewService;
import com.platform.accident.review.api.dto.CaseFileResponse;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.identity.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private final ReviewQueryService queryService;

    @GetMapping("/{caseId}")
//    public ResponseEntity<CaseFileResponse> getCaseFile(@PathVariable String caseId) {
//        return ResponseEntity.ok(reviewService.getConsolidatedCaseFile(caseId));
//    }
    public ResponseEntity<CaseFileResponse> getCaseFile(
            @PathVariable String caseId,
            HttpServletRequest httpRequest) {

        // Παίρνουμε το έμπιστο context από τον Interceptor
        IdentityContext user = SecurityContext.getRequired(httpRequest);

        // Το στέλνουμε στο Service για να γίνει ο έλεγχος permissions
        return ResponseEntity.ok(reviewService.getConsolidatedCaseFile(caseId, user));
    }

    @PostMapping("/{caseId}/lock")
    public ResponseEntity<Void> lock(@PathVariable String caseId, HttpServletRequest request) {
        // Derive Context
        IdentityContext agentCtx = (IdentityContext) request.getAttribute("USER_CONTEXT");

        if (agentCtx == null) return ResponseEntity.status(401).build();

        // Business Service now takes the full IdentityContext for capability checking
        reviewService.lockCase(caseId, agentCtx);
        return ResponseEntity.accepted().build();
    }


    @PostMapping("/{caseId}/verify")
    public ResponseEntity<Void> verify(
            @PathVariable String caseId,
            HttpServletRequest request, // Header trust removed
            @RequestBody Map<String, Object> corrections) {

        IdentityContext context = SecurityContext.getRequired(request);
        reviewService.verifyCase(caseId, context, corrections);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/queue")
    public ResponseEntity<List<String>> getQueue() {
        return ResponseEntity.ok(queryService.getPendingReviewQueue());
    }
}