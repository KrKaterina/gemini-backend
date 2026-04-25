package com.platform.accident.submission.integration;


/**
 * Internal interface used specifically by the Intelligence Module
 * to fetch binary bytes for AI analysis.
 */
public interface AiMediaClient {

   AiMediaResource getAssetResource(String assetId);

}