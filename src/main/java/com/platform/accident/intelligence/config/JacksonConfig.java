package com.platform.accident.intelligence.config;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register support for Java 8 Dates/Times (Instant, LocalDate, etc.)
        mapper.registerModule(new JavaTimeModule());

        // CRITICAL FOR AI: LLMs often hallucinate extra fields in JSON.
        // This prevents the application from crashing if the AI adds fields
        // that aren't in your Java Records.
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Accept single values for arrays (sometimes AI wraps things oddly)
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        return mapper;
    }
}