package com.platform.accident.intelligence.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Bean
    public Client googleGenAiClient() {
        // Θα τραβήξει αυτόματα το API KEY από το environment variable GOOGLE_API_KEY
        return new Client();
    }
}