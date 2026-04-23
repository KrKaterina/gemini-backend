package com.platform.integration.media;

import java.util.List;

/**
 * Contract used by Module 1 to finalize asset ownership.
 */
public interface MediaAssetClient {
    void linkAssetsToCase(String caseId, List<String> assetIds);

    void verifyAssetsExist(List<String> assetIds);

    List<MediaMetadataView> getAssetsByCase(String caseId);
}