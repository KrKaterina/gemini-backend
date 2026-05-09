package com.platform.accident.intelligence.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalysisLogTest {

    @Test
    @DisplayName("Should correctly set and get all fields")
    void testGettersAndSetters() {
        // 1. Create instance
        AiAnalysisLog log = new AiAnalysisLog();
        Instant now = Instant.now();

        // 2. Set values
        log.setId("log-1");
        log.setCaseId("case-123");
        log.setModelId("gpt-4o");
        log.setProcessedAt(now);
        log.setDurationMs(1500L);
        log.setSuccessful(true);
        log.setSentPrompt("Analyzing accident...");
        log.setErrorDetail(null);

        // 3. Assert (Check values)
        assertThat(log.getId()).isEqualTo("log-1");
        assertThat(log.getCaseId()).isEqualTo("case-123");
        assertThat(log.getModelId()).isEqualTo("gpt-4o");
        assertThat(log.getProcessedAt()).isEqualTo(now);
        assertThat(log.getDurationMs()).isEqualTo(1500L);
        assertThat(log.isSuccessful()).isTrue(); // Σημείωση: για boolean ο getter λέγεται isSuccessful()
        assertThat(log.getSentPrompt()).isEqualTo("Analyzing accident...");
        assertThat(log.getErrorDetail()).isNull();
    }

    @Test
    @DisplayName("Should verify equals and hashCode equality")
    void testEqualsAndHashCode() {
        AiAnalysisLog log1 = new AiAnalysisLog();
        log1.setId("id-1");
        log1.setCaseId("C1");

        AiAnalysisLog log2 = new AiAnalysisLog();
        log2.setId("id-1");
        log2.setCaseId("C1");

        // Λόγω της @Data, αν τα περιεχόμενα είναι ίδια, τα objects είναι ίσα
        assertThat(log1).isEqualTo(log2);
        assertThat(log1.hashCode()).isEqualTo(log2.hashCode());
    }

    @Test
    @DisplayName("ToString should contain important field values")
    void testToString() {
        AiAnalysisLog log = new AiAnalysisLog();
        log.setId("id-X");
        log.setCaseId("case-X");

        String toStringResult = log.toString();

        assertThat(toStringResult)
                .contains("id-X")
                .contains("case-X")
                .contains("AiAnalysisLog");
    }
}