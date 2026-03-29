package com.platform.accident.review.api;

import com.platform.accident.review.api.dto.*;
import com.platform.accident.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class CaseReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{caseId}/lock")
    public ResponseEntity<Void> lockCase(@PathVariable String caseId, @RequestHeader("X-Agent-Id") String agentId) {
        reviewService.lockCase(caseId, agentId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{caseId}/verify")
    public ResponseEntity<Void> verifyCase(
            @PathVariable String caseId,
            @RequestHeader("X-Agent-Id") String agentId,
            @RequestBody VerifyRequest request) {

        reviewService.verifyCase(caseId, agentId, request.finalSeverity(), request.adjustments());
        return ResponseEntity.accepted().build();
    }
}