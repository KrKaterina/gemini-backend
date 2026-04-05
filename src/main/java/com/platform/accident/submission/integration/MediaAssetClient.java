package com.platform.accident.submission.integration;

import com.platform.accident.submission.domain.Location;
import java.util.List;
import java.util.Map;

/**
 * Interface implemented by the Media Module.
 */
public interface MediaAssetClient {
    void linkAssetsToCase(String caseId, List<String> assetIds);
    void verifyAssetsExist(List<String> assetIds);
}
