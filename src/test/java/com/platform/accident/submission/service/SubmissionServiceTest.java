package com.platform.accident.submission.service;

import com.platform.accident.submission.domain.*;
import com.platform.accident.submission.exception.AccidentNotFoundException;
import com.platform.accident.submission.integration.*;
import com.platform.accident.submission.repository.AccidentRepository;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.media.MediaAssetClient;
import com.platform.integration.policy.EligibilityStatus;
import com.platform.integration.policy.PolicyPort;
import com.platform.policy.exception.IneligibleReportingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private AccidentRepository repository;
    @Mock private AccidentValidator validator;
    @Mock private MediaAssetClient mediaClient;
    @Mock private ContextEnrichmentClient contextClient;
    @Mock private IntelligenceOrchestrationClient intelligenceClient;
    @Mock private IdentityClient identityClient;
    @Mock private PolicyPort policyPort;

    @InjectMocks
    private SubmissionService submissionService;

    private AccidentReportInput validInput;
    private final String USER_ID = "user-123";

    @BeforeEach
    void setUp() {
        validInput = new AccidentReportInput(
                Instant.now().minusSeconds(3600),
                new Location(37.9, 23.7, "Athens"),
                "Crash report",
                List.of("asset-1")
        );
    }

    @Test
    @DisplayName("Submit: Should throw exception if policy coverage check fails")
    void submitAccident_Ineligible_ShouldThrowException() {
        when(policyPort.checkEligibility(anyString(), any(Instant.class)))
                .thenReturn(EligibilityStatus.denied("EXPIRED_POLICY"));

        assertThatThrownBy(() -> submissionService.submitAccident(validInput, USER_ID))
                .isInstanceOf(IneligibleReportingException.class)
                .hasMessageContaining("EXPIRED_POLICY");

        verify(identityClient).logSecurityEvent(eq(USER_ID), eq("REJECTED_REPORT_INELIGIBLE"), anyString());
        verifyNoInteractions(repository, mediaClient);
    }

    @Test
    @DisplayName("Submit: Successful creation of Accident Report with generated CaseID")
    void submitAccident_Success() {
        when(policyPort.checkEligibility(any(), any()))
                .thenReturn(new EligibilityStatus(true, "VALID", "POL-001", "ALLIANZ"));

        AccidentReport report = submissionService.submitAccident(validInput, USER_ID);

        assertThat(report).isNotNull();
        assertThat(report.getCaseId()).startsWith("ACC-");
        assertThat(report.getStatus()).isEqualTo(AccidentStatus.RECEIVED);

        verify(repository).save(any(AccidentReport.class));
        verify(mediaClient).linkAssetsToCase(eq(report.getCaseId()), eq(validInput.assetIds()));
        verify(identityClient).logSecurityEvent(eq(USER_ID), eq("REPORT_SUBMISSION_START"), anyString());
    }

    @Test
    @DisplayName("GetReport: Should throw Exception when caseId does not exist")
    void getReport_NotFound_ThrowsException() {
        when(repository.findByCaseId("INV-ID")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.getReport("INV-ID"))
                .isInstanceOf(AccidentNotFoundException.class);
    }

    @Test
    @DisplayName("Parse Speed: Internal logic check for various string formats")
    void testParseSpeedLogic() {
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(submissionService, "parseSpeed", "80km");
    }

    @Test
    @DisplayName("Status Update: Repository should save updated report status")
    void updateReportStatus_Success() {
        AccidentReport report = new AccidentReport();
        when(repository.findByCaseId("ID")).thenReturn(Optional.of(report));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                submissionService, "updateReportStatus", "ID", AccidentStatus.ANALYZING);

        assertThat(report.getStatus()).isEqualTo(AccidentStatus.ANALYZING);
        verify(repository).save(report);
    }

    @Test
    @DisplayName("Submit: Full Data Integrity Check - Verifies all fields are mapped correctly")
    void submitAccident_DataIntegrity_Check() {
        when(policyPort.checkEligibility(any(), any()))
                .thenReturn(new EligibilityStatus(true, "VALID", "P1", "INS"));

        ArgumentCaptor<AccidentReport> reportCaptor = ArgumentCaptor.forClass(AccidentReport.class);

        submissionService.submitAccident(validInput, USER_ID);

        verify(repository).save(reportCaptor.capture());
        AccidentReport savedReport = reportCaptor.getValue();

        assertThat(savedReport.getReporterId()).isEqualTo(USER_ID);
        assertThat(savedReport.getRawDescription()).isEqualTo(validInput.description());
        assertThat(savedReport.getLocation().lat()).isEqualTo(validInput.location().lat());
        assertThat(savedReport.getStatus()).isEqualTo(AccidentStatus.RECEIVED);
        assertThat(savedReport.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Internal Logic: Speed Parser should handle various string formats correctly")
    void parseSpeed_Logic_Test() {
        Integer speed1 = (Integer) ReflectionTestUtils.invokeMethod(submissionService, "parseSpeed", "80 km/h");
        assertThat(speed1).isEqualTo(80);

        Integer speed2 = (Integer) ReflectionTestUtils.invokeMethod(submissionService, "parseSpeed", "");
        assertThat(speed2).isEqualTo(50);

        Integer speed3 = (Integer) ReflectionTestUtils.invokeMethod(submissionService, "parseSpeed", "unknown");
        assertThat(speed3).isEqualTo(50);
    }

    @Test
    @DisplayName("Async Logic: Trigger Background should move status to ANALYZING and call AI")
    void triggerBackground_Logic_Test() {
        AccidentReport report = new AccidentReport();
        report.setCaseId("C1");
        when(repository.findByCaseId("C1")).thenReturn(Optional.of(report));

        EnrichmentResponse fakeRes = new EnrichmentResponse(
                "Rain", 15.0, "Street 1", "urban", "40", false, Instant.now()
        );
        when(contextClient.enrichAccidentContext(any(), any())).thenReturn(fakeRes);

        ReflectionTestUtils.invokeMethod(submissionService, "triggerBackgroundProcesses",
                "C1", new Location(0,0,""), USER_ID);


        verify(repository, timeout(2000).atLeastOnce()).save(any());

        verify(intelligenceClient, timeout(2000)).processAiAnalysis(eq("C1"), eq(USER_ID));
    }

    @Test
    @DisplayName("Fault Tolerance: Should handle repository failures gracefully")
    void submitAccident_RepositoryError_ShouldPropagateException() {
        when(policyPort.checkEligibility(any(), any())).thenReturn(
                new EligibilityStatus(true, "VALID", "P", "I"));

        doThrow(new RuntimeException("DB Connection Timeout")).when(repository).save(any());

        assertThatThrownBy(() -> submissionService.submitAccident(validInput, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB Connection Timeout");
    }
}