package com.platform.identity.service;

import com.platform.identity.api.dto.AgentDashboardResponse;
import com.platform.identity.api.dto.CustomerDashboardResponse;
import com.platform.identity.api.dto.DashboardResponse;
import com.platform.integration.identity.DashboardDiscoveryPorts;
import com.platform.integration.identity.IdentityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private DashboardDiscoveryPorts.PolicyMetrics policyPort;
    @Mock private DashboardDiscoveryPorts.AccidentMetrics accidentPort;
    @Mock private DashboardDiscoveryPorts.ReviewMetrics reviewPort;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Generate Dashboard: Should return Agent Profile when user has ROLE_AGENT")
    void generateDashboard_ForAgent_ShouldCallReviewPorts() {
        String userId = "agent_007";
        String userName = "James Bond";
        IdentityContext ctx = new IdentityContext(
                userId, userName, List.of("ROLE_AGENT"), List.of(), "REF", true
        );

        when(reviewPort.getTotalPendingQueue()).thenReturn(CompletableFuture.completedFuture(10L));
        when(reviewPort.getAgentActiveLockCount(userId)).thenReturn(CompletableFuture.completedFuture(2L));

        DashboardResponse response = dashboardService.generateDashboard(ctx);

        assertThat(response).isInstanceOf(AgentDashboardResponse.class);
        AgentDashboardResponse agentRes = (AgentDashboardResponse) response;

        assertThat(agentRes.fullName()).isEqualTo(userName);
        assertThat(agentRes.totalPending()).isEqualTo(10L);
        assertThat(agentRes.myActiveWork()).isEqualTo(2L);

        verify(reviewPort).getTotalPendingQueue();
        verify(reviewPort).getAgentActiveLockCount(userId);
        verifyNoInteractions(policyPort, accidentPort);
    }

    @Test
    @DisplayName("Generate Dashboard: Should return Customer Profile when user has ROLE_CUSTOMER")
    void generateDashboard_ForCustomer_ShouldCallPolicyAndAccidentPorts() {
        String userId = "cust_123";
        String userName = "John Doe";
        IdentityContext ctx = new IdentityContext(
                userId, userName, List.of("ROLE_CUSTOMER"), List.of(), "REF", true
        );

        when(policyPort.getOnboardingStatus(userId)).thenReturn(CompletableFuture.completedFuture("ACTIVE"));
        when(accidentPort.getActiveClaimCount(userId)).thenReturn(CompletableFuture.completedFuture(5L));

        DashboardResponse response = dashboardService.generateDashboard(ctx);

        assertThat(response).isInstanceOf(CustomerDashboardResponse.class);
        CustomerDashboardResponse custRes = (CustomerDashboardResponse) response;

        assertThat(custRes.fullName()).isEqualTo(userName);
        assertThat(custRes.onboardingStatus()).isEqualTo("ACTIVE");
        assertThat(custRes.claimCount()).isEqualTo(5L);

        verify(policyPort).getOnboardingStatus(userId);
        verify(accidentPort).getActiveClaimCount(userId);
        verifyNoInteractions(reviewPort);
    }
}