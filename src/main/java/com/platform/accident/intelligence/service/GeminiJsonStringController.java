package com.platform.accident.intelligence.service;


import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/test-gemini")
@RequiredArgsConstructor
public class GeminiJsonStringController {

    private final Client googleGenAiClient;

    @PostMapping(value = "/just-text", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String analyzeWithPlainText(
            @RequestPart("combinedText") String combinedText, // Εδώ θα μπει το "ενωμένο" κείμενό σου
            @RequestPart("image") MultipartFile image
    ) throws Exception {

        // Φτιάχνουμε τα Parts - Στέλνουμε μόνο καθαρό κείμενο
    Part textPart = Part.builder().text(combinedText).build();

    Part imagePart = Part.builder()
            .inlineData(Blob.builder()
                    .mimeType(image.getContentType())
                    .data(image.getBytes())
                    .build())
            .build();

    Content content = Content.builder()
            .parts(List.of(textPart, imagePart))
            .build();


        // Κλήση στο Gemini
        GenerateContentResponse response = googleGenAiClient.models.generateContent(
                "gemini-2.5-flash",
                content,
                null
        );

        return response.text();
    }
}