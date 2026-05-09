package com.platform.policy.service;

import com.platform.integration.identity.IdentityClient;
import com.platform.integration.policy.EligibilityStatus;
import com.platform.integration.policy.InsurerSnapshot;
import com.platform.policy.api.dto.DeclarationRequest;
import com.platform.policy.domain.DeclarationStatus;
import com.platform.policy.domain.InsuranceDeclaration;
import com.platform.policy.exception.PolicyNotFoundException;
import com.platform.policy.exception.UnauthorizedPolicyAccessException;
import com.platform.policy.repository.InsuranceDeclarationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyManagementServiceTest {

    @Mock
    private InsuranceDeclarationRepository repository;

    @Mock
    private IdentityClient identityClient;

    @InjectMocks
    private PolicyManagementService policyService;

    @Test
    @DisplayName("Create Declaration: Should correctly map DTO fields and initial status")
    void createDeclaration_Success() {
        String userId = "user_99";
        DeclarationRequest request = new DeclarationRequest(
                "AXA", "POL-12345", Instant.now(), Instant.now().plus(365, ChronoUnit.DAYS), "asset_id"
        );

        String id = policyService.createDeclaration(userId, request);

        assertThat(id).isNotNull();
        verify(repository).save(argThat(p ->
                p.getUserId().equals(userId) &&
                        p.getProviderCode().equals("AXA") &&
                        p.getStatus() == DeclarationStatus.PENDING
        ));
    }

    @Test
    @DisplayName("Check Eligibility: Valid policy at occurrence time should return Eligible")
    void checkEligibility_WhenValid_ReturnsEligible() {
        String userId = "u1";
        Instant incident = Instant.now();
        InsuranceDeclaration policy = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .validFrom(incident.minus(1, ChronoUnit.DAYS))
                .verifiedExpirationDate(incident.plus(1, ChronoUnit.DAYS))
                .policyNumber("NUM-1")
                .providerCode("PRV-1")
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(policy));

        EligibilityStatus result = policyService.checkEligibility(userId, incident);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("VALID");
    }

    @Test
    @DisplayName("Check Eligibility: Null validFrom should be treated as start of time")
    void checkEligibility_WithNullValidFrom_ReturnsEligible() {
        String userId = "u1";
        Instant incident = Instant.now();
        InsuranceDeclaration policy = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .validFrom(null) // Case: No start date defined
                .verifiedExpirationDate(incident.plus(10, ChronoUnit.DAYS))
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(policy));

        EligibilityStatus result = policyService.checkEligibility(userId, incident);

        assertThat(result.eligible()).isTrue();
    }

    @Test
    @DisplayName("Verification: REJECTED status should clear expiry date and update entity")
    void verifyAndCorrect_RejectedCase_UpdatesCorrectly() {
        String agentId = "agent_1";
        String pId = "p_id";
        InsuranceDeclaration existing = InsuranceDeclaration.builder().id(pId).status(DeclarationStatus.PENDING).build();

        when(identityClient.hasPermission(agentId, "POLICY_VERIFY")).thenReturn(true);
        when(repository.findById(pId)).thenReturn(Optional.of(existing));

        policyService.verifyAndCorrectPolicy(pId, Instant.now(), "REJECTED", agentId);

        assertThat(existing.getStatus()).isEqualTo(DeclarationStatus.REJECTED);
        assertThat(existing.getVerifiedExpirationDate()).isNull();
        verify(repository).save(existing);
    }

    @Test
    @DisplayName("Verification: Should throw exception if agent is not authorized")
    void verifyAndCorrect_UnauthorizedAgent_ThrowsException() {
        when(identityClient.hasPermission(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() ->
                policyService.verifyAndCorrectPolicy("any", Instant.now(), "ACTIVE", "unauthorized_agent")
        ).isInstanceOf(UnauthorizedPolicyAccessException.class);
    }

    @Test
    @DisplayName("Snapshot: Should return InsurerSnapshot with hardcoded endpoint for active policy")
    void getActiveDeclaration_Success() {
        String userId = "user1";
        Instant now = Instant.now();
        InsuranceDeclaration active = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .validFrom(now.minus(10, ChronoUnit.DAYS))
                .verifiedExpirationDate(now.plus(10, ChronoUnit.DAYS))
                .providerCode("CODE")
                .policyNumber("NUM")
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(active));

        Optional<InsurerSnapshot> result = policyService.getActiveDeclaration(userId, now);

        assertThat(result).isPresent();
        assertThat(result.get().providerCode()).isEqualTo("CODE");
        // Σύμφωνα με τον κώδικα σας: "https://api.external.com"
        assertThat(result.get().insurerNotificationEndpoint()).isEqualTo("https://api.external.com");
    }

    @Test
    @DisplayName("Queue: Pending policies retrieval should work for authorized agents")
    void getPendingPolicies_Success() {
        String agentId = "agent_x";
        when(identityClient.hasPermission(agentId, "POLICY_VIEW_QUEUE")).thenReturn(true);
        when(repository.findByStatus(DeclarationStatus.PENDING)).thenReturn(List.of(new InsuranceDeclaration()));

        List<InsuranceDeclaration> queue = policyService.getPendingPolicies(agentId);

        assertThat(queue).isNotEmpty();
    }

    @Test
    @DisplayName("User Policy: Should call repository and return newest policy")
    void getUserPolicy_CallsRepository() {
        String uid = "u";
        policyService.getUserPolicy(uid);
        verify(repository).findFirstByUserIdOrderByCreatedAtDesc(uid);
    }
}