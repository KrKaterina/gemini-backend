package com.platform.policy.integration;

import com.platform.policy.domain.DeclarationStatus;
import com.platform.policy.domain.InsuranceDeclaration;
import com.platform.policy.repository.InsuranceDeclarationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyDashboardAdapterTest {

    @Mock
    private InsuranceDeclarationRepository repository;

    @InjectMocks
    private PolicyDashboardAdapter dashboardAdapter;

    @Test
    @DisplayName("Onboarding Status: Should return ACTIVE when user has an active policy")
    void getOnboardingStatus_ReturnsActive() {
        // Arrange
        String userId = "user-1";
        InsuranceDeclaration activeDecl = InsuranceDeclaration.builder()
                .status(DeclarationStatus.ACTIVE)
                .build();

        when(repository.findFirstByUserId(userId)).thenReturn(Optional.of(activeDecl));

        // Act
        CompletableFuture<String> futureResult = dashboardAdapter.getOnboardingStatus(userId);
        String result = futureResult.join(); // Περιμένουμε το αποτέλεσμα του Future

        // Assert
        assertThat(result).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Onboarding Status: Should return PENDING when policy exists but is not active")
    void getOnboardingStatus_ReturnsPending() {
        // Arrange
        String userId = "user-2";
        InsuranceDeclaration pendingDecl = InsuranceDeclaration.builder()
                .status(DeclarationStatus.PENDING)
                .build();

        when(repository.findFirstByUserId(userId)).thenReturn(Optional.of(pendingDecl));

        // Act
        String result = dashboardAdapter.getOnboardingStatus(userId).join();

        // Assert
        assertThat(result).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Onboarding Status: Should return NONE when no policy record is found for user")
    void getOnboardingStatus_ReturnsNone() {
        // Arrange
        String userId = "user-unknown";
        when(repository.findFirstByUserId(userId)).thenReturn(Optional.empty());

        // Act
        String result = dashboardAdapter.getOnboardingStatus(userId).join();

        // Assert
        assertThat(result).isEqualTo("NONE");
    }
}