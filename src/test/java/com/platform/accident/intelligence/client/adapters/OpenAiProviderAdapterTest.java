package com.platform.accident.intelligence.client.adapters;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAiProviderAdapterTest {

    @Mock
    private AiSchemaEnforcer schemaEnforcer;

    @InjectMocks
    private OpenAiProviderAdapter adapter;

    @Test
    @DisplayName("Provider Name: Should return gpt-4o as the provider string")
    void getProviderName_ShouldReturnCorrectName() {
        assertThat(adapter.getProviderName()).isEqualTo("gpt-4o");
    }

    @Test
    @DisplayName("Analyze: Should invoke enforcer with simulated JSON output and return structured result")
    void analyzeIncident_ShouldProcessSimulationCorrectly() {
        String prompt = "Incident at Main St";
        List<String> assetIds = List.of("img1");

        AiIntelligenceResult mockResult = new AiIntelligenceResult(
                "AI generated summary",
                null,
                "MEDIUM",
                List.of("Step 1"),
                "Full transcript"
        );

        when(schemaEnforcer.enforceSchema(anyString())).thenReturn(mockResult);

        AiIntelligenceResult result = adapter.analyzeIncident(prompt, assetIds);

        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("AI generated summary");
        assertThat(result.severityLevel()).isEqualTo("MEDIUM");

        verify(schemaEnforcer).enforceSchema(argThat(json -> json.contains("Audi") && json.contains("ABC-123")));
    }
}