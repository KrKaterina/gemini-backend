package com.platform.accident.intelligence.client.adapters;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.platform.accident.intelligence.client.AiModelProvider;
import com.platform.accident.intelligence.domain.AiIntelligenceResult;
import com.platform.accident.intelligence.service.AiSchemaEnforcer;
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

   @Override
//    public AiIntelligenceResult analyzeIncident(String prompt, List<byte[]> images, byte[] audio) {
//        try {
//            log.info("Sending combined text and image to Gemini...");
//
//            List<Part> parts = new ArrayList<>();
//
//            // Το Prompt είναι το combined text που έφτιαξε ο Orchestrator
//            parts.add(Part.builder().text(prompt).build());
//
//            if (images != null && !images.isEmpty()) {
//                parts.add(Part.builder()
//                        .inlineData(Blob.builder()
//                                .mimeType("image/jpeg")
//                                .data(images.get(0))
//                                .build())
//                        .build());
//            }
//
//            Content content = Content.builder().parts(parts).build();
//
//            GenerateContentResponse response = googleGenAiClient.models.generateContent(
//                    "gemini-2.5-flash", // Χρησιμοποίησε 2.0-flash (το 2.5 ίσως σου δημιουργήσει θέματα)
//                    content,
//                    null
//            );
//
//            String rawOutput = response.text();
//
//            return schemaEnforcer.enforceSchema(rawOutput);
//
//        } catch (Exception e) {
//            log.error("AI failed: {}", e.getMessage());
//            throw new RuntimeException(e);
//        }
//    }
public AiIntelligenceResult analyzeIncident(String consolidatedPrompt, List<byte[]> images, byte[] audio) {
    try {
        log.info("Sending expert reconstruction request to Gemini (Plain Text)...");

        String expertPrompt = consolidatedPrompt +
                "\n\nPlease provide a full professional accident reconstruction analysis. " +
                "In your conclusion, you must clearly state a SEVERITY LEVEL as one of these: [Low, Medium, High, Fatal].";

        List<Part> parts = new ArrayList<>();
        parts.add(Part.builder().text(expertPrompt).build());

        if (images != null && !images.isEmpty()) {
            parts.add(Part.builder()
                    .inlineData(Blob.builder()
                            .mimeType("image/jpeg")
                            .data(images.get(0))
                            .build())
                    .build());
        }

                   Content content = Content.builder().parts(parts).build();

        GenerateContentResponse response = googleGenAiClient.models.generateContent(
                    "gemini-2.5-flash",
                    content,
                    null
            );

        String rawAiText = response.text();

        // Στέλνουμε το κείμενο σε μια νέα μέθοδο για να πάρει το Severity.
        return mapRawTextToResult(rawAiText);

    } catch (Exception e) {
        log.error("AI failed: {}", e.getMessage());
        throw new RuntimeException(e);
    }
}

    @Override
    public String getProviderName() {
        return "gemini";
    }


    // αξιολόγηση απάντησης
    private AiIntelligenceResult mapRawTextToResult(String rawText) {
        // Ψάχνουμε μέσα στο κείμενο αν το AI έγραψε κάποια από τις λέξεις κλειδιά
        String severity = "UNKNOWN";
        String upperText = rawText.toUpperCase();

        if (upperText.contains("FATAL")) severity = "FATAL";
        else if (upperText.contains("HIGH")) severity = "HIGH";
        else if (upperText.contains("MEDIUM")) severity = "MEDIUM";
        else if (upperText.contains("LOW")) severity = "LOW";

        // Επιστρέφουμε το Record έτοιμο για τη βάση
        return new AiIntelligenceResult(
                "Expert Analysis complete",
                null,
                severity,
                List.of("Follow instructions in full report"),
                rawText
        );
    }
}









