package com.platform.accident.review.api;

import com.platform.accident.review.service.ReviewService;
import com.platform.accident.review.api.dto.CaseFileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{caseId}")
//    public ResponseEntity<Map<String, Object>> getCaseFile(@PathVariable String caseId) {
//        return ResponseEntity.ok(reviewService.getConsolidatedCaseFile(caseId));
//    }
    public ResponseEntity<CaseFileResponse> getCaseFile(@PathVariable String caseId) {
        return ResponseEntity.ok(reviewService.getConsolidatedCaseFile(caseId));
    }

    @PostMapping("/{caseId}/lock")
    public ResponseEntity<Void> lock(@PathVariable String caseId, @RequestHeader("X-Agent-Id") String agentId) {
        reviewService.lockCase(caseId, agentId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{caseId}/verify")
    public ResponseEntity<Void> verify(
            @PathVariable String caseId,
            @RequestHeader("X-Agent-Id") String agentId,
            @RequestBody Map<String, Object> corrections) {
        reviewService.verifyCase(caseId, agentId, corrections);
        return ResponseEntity.ok().build();
    }
}