package com.platform.accident.intelligence.client.adapters;


import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.*;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class OpenAiProviderAdapter implements AiModelProvider {

    private final AiSchemaEnforcer schemaEnforcer;

    @Override
    public AiIntelligenceResult analyzeIncident(String prompt) {

        String rawResponse = simulateProviderCall(prompt);
        return schemaEnforcer.enforceSchema(rawResponse);
    }

    private String simulateProviderCall(String prompt) {
        // In a real implementation, this uses RestClient or Spring AI
        return """
        {
          "summary": "AI generated summary of the incident...",
          "entities": {
            "vehicles": [{"make": "Audi", "plate": "ABC-123", "role": "Victim"}],
            "persons": [{"role": "Driver", "description": "Witness 1"}],
            "identifiedLocation": "Main St intersection"
          },
          "severityLevel": "MEDIUM",
          "suggestedNextSteps": ["Verify damage photos", "Call driver"],
          "rawAiOutput": "Full transcript from provider"
        }
        """;
    }

    @Override
    public String getProviderName() {
        return "gpt-4o";
    }
}