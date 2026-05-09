package com.platform.accident.intelligence.client.adapters;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
import com.platform.accident.submission.integration.AiMediaClient;
import com.platform.accident.submission.integration.AiMediaResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiProviderAdapterTest {

    @Mock private Client googleGenAiClient;
    @Mock private AiSchemaEnforcer schemaEnforcer;
    @Mock private AiMediaClient mediaClient;

    @Mock private com.google.genai.Models modelsMock;
    @Mock private com.google.genai.Files filesMock;

    @InjectMocks
    private GeminiProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleGenAiClient, "models", modelsMock);
        ReflectionTestUtils.setField(googleGenAiClient, "files", filesMock);
    }

    @Test
    @DisplayName("Analyze: Success with Multimodal Evidence (Image/Text)")
    void analyzeIncident_FullSuccess() throws IOException {
        String assetId = "asset-001";
        String aiResponseText = "{\"summary\": \"crash detected\"}";

        byte[] dummyBytes = "fake-image-binary".getBytes();
        AiMediaResource resource = new AiMediaResource(
                new ByteArrayInputStream(dummyBytes), "crash.jpg", "image/jpeg", (long) dummyBytes.length);

        when(mediaClient.getAssetResource(assetId)).thenReturn(resource);

        File mockUploadedFile = mock(File.class);
        when(mockUploadedFile.uri()).thenReturn(Optional.of("gs://bucket/asset-001"));

        when(filesMock.upload(any(byte[].class), any(UploadFileConfig.class))).thenReturn(mockUploadedFile);

        GenerateContentResponse genResponse = mock(GenerateContentResponse.class);
        when(genResponse.text()).thenReturn(aiResponseText);
        when(modelsMock.generateContent(anyString(), any(Content.class), isNull())).thenReturn(genResponse);

        AiIntelligenceResult mockFinalResult = new AiIntelligenceResult(
                "Technical Summary", null, "MEDIUM", List.of("Next Step"), "Reasoning");
        when(schemaEnforcer.enforceSchema(aiResponseText)).thenReturn(mockFinalResult);

        AiIntelligenceResult result = adapter.analyzeIncident("Analyze", List.of(assetId));

        assertThat(result).isNotNull();
        assertThat(result.severityLevel()).isEqualTo("MEDIUM");
        assertThat(result.summary()).isEqualTo("Technical Summary");

        verify(filesMock, times(1)).upload(any(byte[].class), any());
    }

    @Test
    @DisplayName("Retry: Should handle 503 Overload and eventual fallback")
    void analyzeIncident_RetryOn503() {
        when(modelsMock.generateContent(anyString(), any(Content.class), isNull()))
                .thenThrow(new RuntimeException("Error: 503 - Google Server Overloaded"));

        AiIntelligenceResult result = adapter.analyzeIncident("prompt", List.of());

        assertThat(result.severityLevel()).isEqualTo("UNKNOWN");
        assertThat(result.rawAiOutput()).contains("Error 503");

        verify(modelsMock, times(3)).generateContent(anyString(), any(Content.class), isNull());
    }

    @Test
    @DisplayName("Info: Verify Provider Name")
    void getProviderName_ShouldBeGemini() {
        assertThat(adapter.getProviderName()).isEqualTo("gemini");
    }
}