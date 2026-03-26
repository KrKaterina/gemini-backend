package com.platform.accident.intelligence.client;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;

import java.util.List;

/**
 * Abstraction to allow switching between OpenAI, Anthropic, or local LLMs.
 */
public interface AiModelProvider {
    // Προσθήκη εικόνων και ήχου στο signature
    AiIntelligenceResult analyzeIncident(String consolidatedPrompt, List<byte[]> images, byte[] audio);
    String getProviderName();
}