package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import java.util.List;
import java.util.Map;

/**
 * Interface implemented by the Media Module.
 */
public interface MediaAssetClient {
    // Standardizing the naming to match SubmissionService's calls
    void linkAssetsToCase(String caseId, List<String> assetIds);

    // Ensure this exists because the SubmissionService Validator calls it
    void verifyAssetsExist(List<String> assetIds);
    byte[] getAssetBytes(String assetId);
}