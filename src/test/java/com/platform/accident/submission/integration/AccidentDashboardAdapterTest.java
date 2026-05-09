package com.platform.accident.submission.integration;

import com.platform.accident.submission.repository.AccidentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccidentDashboardAdapterTest {

    @Mock
    private AccidentRepository repository;

    @InjectMocks
    private AccidentDashboardAdapter adapter;

    @Test
    @DisplayName("Metrics: Should return the correct count of claims for a user via CompletableFuture")
    void getActiveClaimCount_Success() throws ExecutionException, InterruptedException {
        String userId = "reporter-123";
        long expectedCount = 5L;

        when(repository.countByReporterId(userId)).thenReturn(expectedCount);

        CompletableFuture<Long> resultFuture = adapter.getActiveClaimCount(userId);

        assertThat(resultFuture).isNotNull();
        assertThat(resultFuture.get()).isEqualTo(expectedCount);

        verify(repository, times(1)).countByReporterId(userId);
    }
}