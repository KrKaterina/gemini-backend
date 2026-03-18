package com.platform.accident.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSchemaEnforcer {

    @Autowired
    private final ObjectMapper objectMapper;

    /**
     * Parses raw JSON from the AI provider into structured records.
     */
    public AiIntelligenceResult enforceSchema(String rawOutput) {
        try {
            // Logic to clean the common AI 'markdown' wrapping if present
            String json = rawOutput.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(json, AiIntelligenceResult.class);
        } catch (Exception e) {
            log.error("Failed to parse AI output. Content: {}", rawOutput);
            throw new RuntimeException("AI_SCHEMA_MISMATCH", e);
        }
    }
}