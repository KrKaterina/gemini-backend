package com.platform.identity.service;

import com.platform.identity.api.dto.*;
import com.platform.integration.identity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardDiscoveryPorts.PolicyMetrics policyPort;
    private final DashboardDiscoveryPorts.AccidentMetrics accidentPort;
    private final DashboardDiscoveryPorts.ReviewMetrics reviewPort;

    public DashboardResponse generateDashboard(IdentityContext ctx) {
        String userId = ctx.userId();
        String name = ctx.username(); // Or fetch from local Profile entity

        if (ctx.roles().contains("ROLE_AGENT")) {
            return buildAgentDashboard(userId, name);
        } else {
            return buildCustomerDashboard(userId, name);
        }
    }

    private DashboardResponse buildCustomerDashboard(String userId, String name) {
        var statusFuture = policyPort.getOnboardingStatus(userId);
        var claimsFuture = accidentPort.getActiveClaimCount(userId);

        CompletableFuture.allOf(statusFuture, claimsFuture).join();

        return new CustomerDashboardResponse(
                "ROLE_CUSTOMER",
                name,
                statusFuture.join(),
                claimsFuture.join()
        );
    }

    private DashboardResponse buildAgentDashboard(String agentId, String name) {
        var pendingFuture = reviewPort.getTotalPendingQueue();
        var myWorkFuture = reviewPort.getAgentActiveLockCount(agentId);

        CompletableFuture.allOf(pendingFuture, myWorkFuture).join();

        return new AgentDashboardResponse(
                "ROLE_AGENT",
                name,
                pendingFuture.join(),
                myWorkFuture.join()
        );
    }
}