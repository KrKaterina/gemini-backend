package com.platform.accident.intelligence.client.adapters;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
import com.platform.accident.submission.integration.AiAssetData;
import com.platform.accident.submission.integration.AiMediaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProviderAdapter implements AiModelProvider {

    private final Client googleGenAiClient;
    private final AiSchemaEnforcer schemaEnforcer;

    private final AiMediaClient mediaClient; // Bridge to binary module

    @Override
//    public AiIntelligenceResult analyzeIncident(String consolidatedPrompt) {
//        try {
//            log.info("Sending expert reconstruction request to Gemini...");
//
//            String expertPrompt = """
//                        You are an expert accident reconstruction analyst.
//
//                        Analyze the following incident and RETURN ONLY VALID JSON.
//                        - If unknown, use empty string "" instead of null
//
//                        STRICT RULES:
//                        - Do NOT return markdown
//                        - Do NOT wrap in ```json
//                        - Do NOT add explanations outside JSON
//                        - Always include all fields
//
//                        JSON SCHEMA:
//                        {
//                          "summary": "concise professional summary",
//                          "entities": {
//                            "vehicles": [
//                              {"make": "", "plate": "", "role": ""}
//                            ],
//                            "persons": [
//                              {"role": "", "description": ""}
//                            ],
//                            "identifiedLocation": ""
//                          },
//                          "severityLevel": "LOW|MEDIUM|HIGH|FATAL",
//                          "suggestedNextSteps": [],
//                          "rawAiOutput": "full detailed reasoning"
//                        }
//
//                        CLASSIFICATION RULES:
//                        - LOW → no injuries
//                        - MEDIUM → minor injuries
//                        - HIGH → serious injuries
//                        - FATAL → death involved
//
//                        Incident data:
//                        """ + consolidatedPrompt;
//
//            Content content = Content.builder()
//                    .parts(List.of(Part.builder().text(expertPrompt).build()))
//                    .build();
//
//            GenerateContentResponse response = googleGenAiClient.models.generateContent(
//                        "gemini-2.5-flash",
//                        content,
//                        null
//                );
//
//            String rawAiText = response.text();
//            log.info("Gemini RAW response received ({} chars)", rawAiText.length());
//
//            return schemaEnforcer.enforceSchema(rawAiText);
//
//        } catch (Exception e) {
//            log.error("AI failed: {}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
    public AiIntelligenceResult analyzeIncident(String textPrompt, List<String> assetIds) {
        try {
            List<Part> parts = new ArrayList<>();

            // 1. Add Expert Analysis Text Prompt
            parts.add(Part.builder().text(textPrompt).build());

            // 2. FETCH AND ATTACH VISUAL EVIDENCE (Multimodality)
            assetIds.forEach(id -> {
                byte[] bytes = mediaClient.getAssetBytes(id);
                // Currently optimized for JPEGs as per business case
                parts.add(Part.builder()
                        .inlineData(Blob.builder().data(bytes).mimeType("image/jpeg").build())
                        .build());
                log.info("Visual evidence {} attached to prompt.", id);
            });

            Content content = Content.builder().parts(parts).build();
            GenerateContentResponse response = googleGenAiClient.models.generateContent(
                    "gemini-2.5-flash", content, null
            );

            return schemaEnforcer.enforceSchema(response.text());
        } catch (Exception e) {
            log.error("AI Multimodal Analysis failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

}









