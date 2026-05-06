package com.platform.accident.review.api;

import com.platform.accident.review.api.dto.ReviewQueueItem;
import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.service.ReviewQueryService;
import com.platform.accident.review.service.ReviewService;
import com.platform.accident.review.api.dto.CaseFileResponse;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.identity.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final IdentityClient identityClient;

    @GetMapping("/{caseId}")
    public ResponseEntity<CaseFileResponse> getCaseFile(
            @PathVariable String caseId,
            HttpServletRequest httpRequest) {

        IdentityContext user = SecurityContext.getRequired(httpRequest);

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

    @GetMapping("/dashboard")
    public ResponseEntity<List<ReviewQueueItem>> getPublicQueue(HttpServletRequest request) {
        // Ensure agent permissions before showing the queue
        var ctx = SecurityContext.getRequired(request);
        if (!identityClient.hasPermission(ctx.userId(), "ACCIDENT_REPORT_VIEW_ALL")){
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(queryService.getAgentDashboard(ReviewStatus.PENDING, null));
    }

    @GetMapping("/my-workspace")
    public ResponseEntity<List<ReviewQueueItem>> getMyWorkspace(HttpServletRequest request) {
        var ctx = SecurityContext.getRequired(request);
        return ResponseEntity.ok(queryService.getAgentDashboard(ReviewStatus.IN_PROGRESS, null));
    }

    @GetMapping("/audit-feed")
    public ResponseEntity<List<ReviewQueueItem>> getAuditHistory(HttpServletRequest request) {
        // IDENTITY DERIVATION
        var ctx = SecurityContext.getRequired(request);

        // ENFORCEMENT: Does the agent have the specific right to see total feed?
        // (Assuming "SYSTEM_AUDIT_VIEW" is registered in Module 6)
        if (!identityClient.hasPermission(ctx.userId(), "ACCIDENT_REPORT_VIEW_ALL")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Logic uses context to differentiate between self-locks and other locks
        List<ReviewQueueItem> feed = queryService.getVerfiedReports(ctx.userId());
        return ResponseEntity.ok(feed);
    }
}