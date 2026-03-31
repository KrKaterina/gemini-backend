package com.platform.accident.intelligence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSchemaEnforcer {

    private final ObjectMapper objectMapper;

    public AiIntelligenceResult enforceSchema(String rawOutput) {
        try {
            String cleanJson = rawOutput.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return objectMapper.readValue(cleanJson, AiIntelligenceResult.class);
        } catch (Exception e) {
            log.error("Schema mapping failed. Raw content: {}", rawOutput);
            throw new RuntimeException("AI_SCHEMA_MISMATCH", e);
        }
    }
}