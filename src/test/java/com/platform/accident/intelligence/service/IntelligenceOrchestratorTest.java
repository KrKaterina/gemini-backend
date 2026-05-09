package com.platform.accident.intelligence.service;

import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.client.factory.AiProviderFactory;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.repository.AiLogRepository;
import com.platform.accident.submission.integration.AnalysisSourceData;
import com.platform.accident.submission.integration.IntelligenceCallbackClient;
import com.platform.accident.submission.integration.ReportViewerClient;
import com.platform.integration.identity.IdentityClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntelligenceOrchestratorTest {

    @Mock private ReportViewerClient reportViewer;
    @Mock private IntelligenceCallbackClient callbackClient;
    @Mock private AiLogRepository logRepository;
    @Mock private AiProviderFactory providerFactory;
    @Mock private IdentityClient identityClient;

    @Mock private AiModelProvider mockAiProvider;

    @InjectMocks
    private IntelligenceOrchestrator orchestrator;

    private final String CASE_ID = "ACC-123";
    private final String TRACE_ID = "user-001";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orchestrator, "providerName", "gemini");
    }

    @Test
    @DisplayName("AI Process: Successful flow from data retrieval to callback completion")
    void processAiAnalysis_Success() {
        AnalysisSourceData sourceData = new AnalysisSourceData(
                "I was driving when a car hit me from behind",
                "CODE_0", // Clear sky
                "motorway",
                "Metaxourgeio",
                true,
                37.9, 23.7,
                Instant.now(),
                CASE_ID,
                List.of("photo-1", "audio-1")
        );

        AiIntelligenceResult aiResult = new AiIntelligenceResult(
                "Rear-end impact", null, "HIGH", List.of("Steps"), "Raw info"
        );

        when(reportViewer.getSourceDataForAnalysis(CASE_ID)).thenReturn(Optional.of(sourceData));
        when(providerFactory.getProvider("gemini")).thenReturn(mockAiProvider);
        when(mockAiProvider.getProviderName()).thenReturn("gemini");
        when(mockAiProvider.analyzeIncident(anyString(), anyList())).thenReturn(aiResult);

        orchestrator.processAiAnalysis(CASE_ID, TRACE_ID);

        verify(mockAiProvider).analyzeIncident(
                argThat(prompt -> prompt.contains("motorway") && prompt.contains("forensic reconstruction")),
                eq(sourceData.assetIds())
        );

        verify(callbackClient).onAnalysisComplete(eq(CASE_ID), eq(aiResult));
        verify(logRepository).save(any());
        verify(identityClient).logSecurityEvent(eq(TRACE_ID), eq("AUTOMATED_AI_PROCESSING"), anyString());
    }

    @Test
    @DisplayName("AI Process: Failure when source report is missing")
    void processAiAnalysis_ReportNotFound() {
        when(reportViewer.getSourceDataForAnalysis(CASE_ID)).thenReturn(Optional.empty());

        orchestrator.processAiAnalysis(CASE_ID, TRACE_ID);

        verify(callbackClient).onAnalysisFailure(CASE_ID, "AI_PROVIDER_ERROR");
        verify(identityClient).logSecurityEvent(eq(TRACE_ID), eq("AI_FAILURE"), contains("Source not found"));
        verifyNoInteractions(providerFactory, logRepository);
    }

    @Test
    @DisplayName("AI Process: Failure when AI Provider throws an exception")
    void processAiAnalysis_AiProviderCrashes() {
        AnalysisSourceData sourceData = mock(AnalysisSourceData.class);
        when(reportViewer.getSourceDataForAnalysis(CASE_ID)).thenReturn(Optional.of(sourceData));
        when(providerFactory.getProvider("gemini")).thenReturn(mockAiProvider);
        when(mockAiProvider.analyzeIncident(any(), any())).thenThrow(new RuntimeException("API Quota Exceeded"));

        orchestrator.processAiAnalysis(CASE_ID, TRACE_ID);

        verify(callbackClient).onAnalysisFailure(CASE_ID, "AI_PROVIDER_ERROR");
        verify(identityClient).logSecurityEvent(eq(TRACE_ID), eq("AI_FAILURE"), contains("Quota Exceeded"));
    }
}