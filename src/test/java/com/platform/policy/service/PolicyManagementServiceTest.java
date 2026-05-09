package com.platform.policy.service;

import com.platform.integration.identity.IdentityClient;
import com.platform.integration.policy.EligibilityStatus;
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
    @DisplayName("Should correctly identify eligible user when policy is active and dates match")
    void checkEligibility_WhenValid_ShouldReturnEligible() {
        String userId = "user123";
        Instant accidentDate = Instant.now();

        InsuranceDeclaration activePolicy = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .validFrom(accidentDate.minus(5, ChronoUnit.DAYS))
                .verifiedExpirationDate(accidentDate.plus(5, ChronoUnit.DAYS))
                .policyNumber("POL-100")
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(activePolicy));

        EligibilityStatus result = policyService.checkEligibility(userId, accidentDate);

        assertThat(result.eligible()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("VALID");
        assertThat(result.policyNumber()).isEqualTo("POL-100");
    }

    @Test
    @DisplayName("Should deny eligibility if the accident occurred after policy expiration")
    void checkEligibility_WhenExpired_ShouldReturnDenied() {
        String userId = "user123";
        Instant accidentDate = Instant.now();

        InsuranceDeclaration expiredPolicy = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .verifiedExpirationDate(accidentDate.minus(1, ChronoUnit.DAYS)) // Έληξε χθες
                .build();

        when(repository.findAllByUserId(userId)).thenReturn(List.of(expiredPolicy));

        EligibilityStatus result = policyService.checkEligibility(userId, accidentDate);

        assertThat(result.eligible()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("NO_ACTIVE_POLICY_FOR_DATE");
    }

    @Test
    @DisplayName("Should throw exception during verification if agent lacks permissions")
    void verifyPolicy_WhenNoPermission_ShouldThrowException() {
        String agentId = "agent007";
        when(identityClient.hasPermission(agentId, "POLICY_VERIFY")).thenReturn(false);

        assertThatThrownBy(() ->
                policyService.verifyAndCorrectPolicy("id", Instant.now(), "ACTIVE", agentId)
        ).isInstanceOf(UnauthorizedPolicyAccessException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully update policy status when agent is authorized")
    void verifyPolicy_WhenAuthorized_ShouldSaveStatus() {
        String agentId = "agent001";
        String policyId = "p-123";
        Instant newExpiry = Instant.now().plus(30, ChronoUnit.DAYS);

        InsuranceDeclaration pending = InsuranceDeclaration.builder()
                .id(policyId)
                .status(DeclarationStatus.PENDING)
                .build();

        when(identityClient.hasPermission(agentId, "POLICY_VERIFY")).thenReturn(true);
        when(repository.findById(policyId)).thenReturn(Optional.of(pending));

        policyService.verifyAndCorrectPolicy(policyId, newExpiry, "ACTIVE", agentId);

        verify(repository).save(argThat(p ->
                p.getStatus() == DeclarationStatus.ACTIVE &&
                        p.getVerifiedExpirationDate().equals(newExpiry) &&
                        p.getVerifiedByAgentId().equals(agentId)
        ));
    }
}
