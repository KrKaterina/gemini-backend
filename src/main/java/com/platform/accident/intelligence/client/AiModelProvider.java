package com.platform.accident.intelligence.client;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;

/**
 * Abstraction to allow switching between OpenAI, Anthropic, or local LLMs.
 */
public interface AiModelProvider {
    AiIntelligenceResult analyzeIncident(String consolidatedPrompt);
    String getProviderName();
}