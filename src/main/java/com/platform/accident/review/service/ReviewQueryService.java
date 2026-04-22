package com.platform.accident.review.service;

import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.repository.ReviewCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewCaseRepository reviewRepo;

    /**
     * Discovery endpoint for the Agent Dashboard.
     */
    public List<String> getPendingReviewQueue() {
        return reviewRepo.findAll().stream()
                .filter(c -> c.getStatus() == ReviewStatus.PENDING)
                .map(com.platform.accident.review.domain.ReviewCase::getCaseId)
                .toList();
    }
}