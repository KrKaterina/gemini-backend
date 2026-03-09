package com.platform.accident.submission.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
class MediaAssetStub implements MediaAssetClient {
    @Override
    public void linkAssetsToCase(String caseId, List<String> assetIds) {
        log.info("[Media Module] Linking {} assets to case {}", assetIds.size(), caseId);
    }
}
