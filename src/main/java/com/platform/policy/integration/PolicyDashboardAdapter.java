package com.platform.policy.integration;

import com.platform.integration.identity.DashboardDiscoveryPorts;
import com.platform.policy.domain.DeclarationStatus;
import com.platform.policy.repository.InsuranceDeclarationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PolicyDashboardAdapter implements DashboardDiscoveryPorts.PolicyMetrics {

    private final InsuranceDeclarationRepository repository;

    @Async
    @Override
    public CompletableFuture<String> getOnboardingStatus(String userId) {
        return repository.findFirstByUserId(userId)
                .map(decl -> decl.getStatus() == DeclarationStatus.ACTIVE ? "ACTIVE" : "PENDING")
                .map(CompletableFuture::completedFuture)
                .orElse(CompletableFuture.completedFuture("NONE"));
    }
}