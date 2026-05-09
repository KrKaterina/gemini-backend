package com.platform.accident.review.integration;

import com.platform.accident.review.domain.ReviewStatus;
import com.platform.accident.review.repository.ReviewCaseRepository;
import com.platform.integration.identity.DashboardDiscoveryPorts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewDashboardAdapterTest {

    @Mock
    private ReviewCaseRepository reviewRepo;

    @InjectMocks
    private ReviewDashboardAdapter reviewDashboardAdapter;

    @BeforeEach
    void setUp() {

    }

    @Test
    void shouldReturnTotalPendingQueue() throws Exception {
        when(reviewRepo.countByStatus(ReviewStatus.PENDING))
                .thenReturn(7L);

        CompletableFuture<Long> result =
                reviewDashboardAdapter.getTotalPendingQueue();

        assertEquals(7L, result.get());

        verify(reviewRepo)
                .countByStatus(ReviewStatus.PENDING);
    }

    @Test
    void shouldReturnAgentActiveLockCount() throws Exception {
        String agentId = "agent-123";

        when(reviewRepo.countByAssignedAgentIdAndStatus(
                agentId,
                ReviewStatus.IN_PROGRESS
        )).thenReturn(3L);

        CompletableFuture<Long> result =
                reviewDashboardAdapter.getAgentActiveLockCount(agentId);

        assertEquals(3L, result.get());

        verify(reviewRepo)
                .countByAssignedAgentIdAndStatus(
                        agentId,
                        ReviewStatus.IN_PROGRESS
                );
    }
}