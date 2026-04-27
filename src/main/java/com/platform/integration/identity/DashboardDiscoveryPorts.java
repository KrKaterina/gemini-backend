package com.platform.integration.identity;

import java.util.concurrent.CompletableFuture;

/**
 * Technical bridges to cross-module data without sharing DB connections.
 */
public interface DashboardDiscoveryPorts {

    interface PolicyMetrics {
        CompletableFuture<String> getOnboardingStatus(String userId);
    }

    interface AccidentMetrics {
        CompletableFuture<Long> getActiveClaimCount(String userId);
    }

    interface ReviewMetrics {
        CompletableFuture<Long> getTotalPendingQueue();
        CompletableFuture<Long> getAgentActiveLockCount(String agentId);
    }
}