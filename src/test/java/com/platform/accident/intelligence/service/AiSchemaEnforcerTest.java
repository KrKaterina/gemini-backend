package com.platform.accident.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSchemaEnforcerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiSchemaEnforcer enforcer = new AiSchemaEnforcer(objectMapper);

    @Test
    @DisplayName("Should parse valid JSON even when wrapped in AI markdown backticks")
    void enforceSchema_WithMarkdown_ShouldParseCorrectly() {
        String rawOutputFromGemini = """
                ```json
                {
                  "summary": "Rear-end collision at high speed",
                  "severityLevel": "HIGH",
                  "suggestedNextSteps": ["Call emergency", "Collect insurance"],
                  "rawAiOutput": "Original LLM thoughts"
                }
                ```
                """;

        AiIntelligenceResult result = enforcer.enforceSchema(rawOutputFromGemini);

        assertThat(result.summary()).isEqualTo("Rear-end collision at high speed");
        assertThat(result.severityLevel()).isEqualTo("HIGH");
        assertThat(result.suggestedNextSteps()).hasSize(2).contains("Call emergency");
        assertThat(result.rawAiOutput()).isEqualTo("Original LLM thoughts");
    }

    @Test
    @DisplayName("Should parse plain JSON string without markdown correctly")
    void enforceSchema_PlainJson_ShouldParseCorrectly() {
        String plainJson = "{\"summary\":\"Short summary\",\"severityLevel\":\"LOW\"}";

        AiIntelligenceResult result = enforcer.enforceSchema(plainJson);

        assertThat(result.summary()).isEqualTo("Short summary");
        assertThat(result.severityLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("Should throw AI_SCHEMA_MISMATCH exception when JSON is invalid")
    void enforceSchema_InvalidJson_ShouldThrowException() {
        String invalidInput = "This is not JSON at all, Gemini hallucinated.";

        assertThatThrownBy(() -> enforcer.enforceSchema(invalidInput))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI_SCHEMA_MISMATCH");
    }

    @Test
    @DisplayName("Should handle empty strings by throwing exception")
    void enforceSchema_EmptyString_ShouldThrowException() {
        assertThatThrownBy(() -> enforcer.enforceSchema(""))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("AI_SCHEMA_MISMATCH");
    }
}