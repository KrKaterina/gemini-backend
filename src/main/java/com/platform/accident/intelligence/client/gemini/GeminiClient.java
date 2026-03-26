package com.platform.accident.intelligence.client.gemini;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//
//import java.util.List;
//
//@Component
//public class GeminiClient {
//
//    private final RestClient restClient;
//    private final String apiKey;
//
//    public GeminiClient(@Value("${gemini.api.key}") String apiKey) {
//        this.apiKey = apiKey;
//        this.restClient = RestClient.builder()
//                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
//                .build();
//    }
//
//    public String generate(String prompt) {
//
//        var request = new GeminiRequest(
//                List.of(new GeminiRequest.Content(
//                        List.of(new GeminiRequest.Content.Part(buildStrictPrompt(prompt)))
//                ))
//        );
//
//
//        var response = restClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/models/gemini-1.5-flash:generateContent")
//                        .queryParam("key", apiKey)
//                        .build())
//                .body(request)
//                .retrieve()
//                .body(GeminiResponse.class);
//
//        return extract(response);
//
//
//    }
//
//    private String extract(GeminiResponse response) {
//        return response.candidates().get(0).content().parts().get(0).text();
//    }
//
//    private String buildStrictPrompt(String prompt) {
//        return """
//        Return ONLY valid JSON. No explanations.
//
//        Schema:
//        {
//          "summary": "string",
//          "entities": {
//            "vehicles": [{"make": "string", "plate": "string", "role": "string"}],
//            "persons": [{"role": "string", "description": "string"}],
//            "identifiedLocation": "string"
//          },
//          "severityLevel": "LOW | MEDIUM | HIGH",
//          "suggestedNextSteps": ["string"],
//          "rawAiOutput": "string"
//        }
//
//        """ + prompt;
//    }
//
//}

//
//import com.google.cloud.vertexai.generativeai.GenerativeModel;
//import com.google.cloud.vertexai.generativeai.ResponseHandler;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class GeminiClient {
//
//    private final GenerativeModel model;
//
//    public String generate(String prompt) {
//        log.info("Generating AI analysis via SDK...");
//
//        try {
//            // Χρησιμοποιούμε το μοντέλο που κάναμε inject από το Config
//            var response = model.generateContent(buildStrictPrompt(prompt));
//
//            // Το ResponseHandler μας δίνει εύκολα το κείμενο
//            return ResponseHandler.getText(response);
//        } catch (Exception e) {
//            log.error("SDK Call failed: {}", e.getMessage());
//            throw new RuntimeException("AI_PROVIDER_ERROR", e);
//        }
//    }

//    private String buildStrictPrompt(String prompt) {
//        return "Return ONLY a JSON object. " +
//                "Schema: { 'summary': 'string', 'severityLevel': 'LOW|MEDIUM|HIGH' }. " +
//                "Prompt: " + prompt;
//    }
//}
