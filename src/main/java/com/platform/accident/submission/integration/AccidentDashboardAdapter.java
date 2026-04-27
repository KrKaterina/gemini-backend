package com.platform.accident.submission.integration;

import com.platform.integration.identity.DashboardDiscoveryPorts;
import com.platform.accident.submission.repository.AccidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class AccidentDashboardAdapter implements DashboardDiscoveryPorts.AccidentMetrics {

    private final AccidentRepository repository;

    @Async
    @Override
    public CompletableFuture<Long> getActiveClaimCount(String userId) {
        // Implementation: counting claims belonging to the specific reporter
        return CompletableFuture.completedFuture(repository.countByReporterId(userId));
    }
}