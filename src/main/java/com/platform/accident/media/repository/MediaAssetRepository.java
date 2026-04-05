package com.platform.accident.media.repository;

import com.platform.accident.media.domain.MediaAsset;
import com.platform.accident.media.domain.AssetStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends MongoRepository<MediaAsset, String> {
    Optional<MediaAsset> findByAssetId(String assetId);
    List<MediaAsset> findAllByAssetIdIn(List<String> assetIds);
    List<MediaAsset> findAllByCaseId(String caseId);
    long countByAssetIdInAndStatus(List<String> assetIds, AssetStatus status);
    List<MediaAsset> findAllByStatusAndCreatedAtBefore(AssetStatus status, Instant dateTime);
}