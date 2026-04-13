package com.platform.accident.submission.integration;


/**
 * Internal interface used specifically by the Intelligence Module
 * to fetch binary bytes for AI analysis.
 */
public interface AiMediaClient {
   byte[] getAssetBytes(String assetId);
 //   AiAssetData getAssetData(String assetId);
}