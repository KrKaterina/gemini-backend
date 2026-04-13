package com.platform.accident.intelligence.client;

import com.platform.accident.intelligence.domain.AiIntelligenceResult;

import java.util.List;

/**
 * Abstraction to allow switching between OpenAI, Anthropic, or local LLMs.
 */
public interface AiModelProvider {
    AiIntelligenceResult analyzeIncident(String prompt, List<String> assetIds);
    String getProviderName();
}
