package com.platform.accident.intelligence.client.adapters;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiProviderAdapter implements AiModelProvider {

    private final Client googleGenAiClient;
    private final AiSchemaEnforcer schemaEnforcer;

    @Override
    public AiIntelligenceResult analyzeIncident(String consolidatedPrompt) {
        try {
            log.info("Sending expert reconstruction request to Gemini...");

            String expertPrompt = """
                        You are an expert accident reconstruction analyst.
                        
                        Analyze the following incident and RETURN ONLY VALID JSON.
                        - If unknown, use empty string "" instead of null 
                        
                        STRICT RULES:
                        - Do NOT return markdown
                        - Do NOT wrap in ```json
                        - Do NOT add explanations outside JSON
                        - Always include all fields
                        
                        JSON SCHEMA:
                        {
                          "summary": "concise professional summary",
                          "entities": {
                            "vehicles": [
                              {"make": "", "plate": "", "role": ""}
                            ],
                            "persons": [
                              {"role": "", "description": ""}
                            ],
                            "identifiedLocation": ""
                          },
                          "severityLevel": "LOW|MEDIUM|HIGH|FATAL",
                          "suggestedNextSteps": [],
                          "rawAiOutput": "full detailed reasoning"
                        }
                        
                        CLASSIFICATION RULES:
                        - LOW → no injuries
                        - MEDIUM → minor injuries
                        - HIGH → serious injuries
                        - FATAL → death involved
                        
                        Incident data:
                        """ + consolidatedPrompt;

            Content content = Content.builder()
                    .parts(List.of(Part.builder().text(expertPrompt).build()))
                    .build();

            GenerateContentResponse response = googleGenAiClient.models.generateContent(
                        "gemini-2.5-flash",
                        content,
                        null
                );

            String rawAiText = response.text();
            log.info("Gemini RAW response received ({} chars)", rawAiText.length());

            return schemaEnforcer.enforceSchema(rawAiText);

        } catch (Exception e) {
            log.error("AI failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

}









