package com.platform.accident.media.service;

import com.platform.accident.media.domain.*;
import com.platform.accident.media.infrastructure.StorageProvider;
import com.platform.accident.media.repository.MediaAssetRepository;
import com.platform.accident.submission.integration.MediaAssetClient;
import com.platform.integration.media.MediaMetadataView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetService implements MediaAssetClient {

    private final MediaAssetRepository assetRepository;
    private final StorageProvider storageProvider;
    private static final List<String> ALLOWED_MIMES = Arrays.asList("image/jpeg", "image/png", "audio/mpeg", "audio/wav");
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB limit
    private final GridFsTemplate gridFsTemplate;

    /**
     * Initial Upload Phase (Invoked by Frontend Controller)
     */
    public String uploadAsset(InputStream stream, String fileName, String mimeType) {
        String assetId = UUID.randomUUID().toString();

        // 1. Store Binary Data via Stream (No memory bloat)
        String storagePath = storageProvider.store(stream, fileName, mimeType);

        // 2. Persist Metadata in "PENDING" status
        MediaAsset asset = MediaAsset.builder()
                .assetId(assetId)
                .fileName(fileName)
                .mimeType(mimeType)
                .status(AssetStatus.PENDING)
                .storageProvider("GRIDFS")
                .storagePath(storagePath)
                .createdAt(Instant.now())
                .build();

        assetRepository.save(asset);
        return assetId;
    }

    /**
     * Called by Multipart Controller.
     * Binary goes directly to GridFS via InputStream.
     */
    public String storePendingAsset(InputStream stream, String name, String type, long size) {
        String assetId = UUID.randomUUID().toString();
        var fileId = gridFsTemplate.store(stream, name, type);

        assetRepository.save(MediaAsset.builder()
                .assetId(assetId).gridFsId(fileId.toString())
                .fileName(name).mimeType(type).fileSize(size)
                .status(AssetStatus.PENDING).createdAt(Instant.now()).build());
        return assetId;
    }

    /**
     * Implementation of MediaAssetClient Port.
     * Links temporary uploads to the final Accident Report case.
     */
    @Override
    public void linkAssetsToCase(String caseId, List<String> assetIds) {
        log.info("Linking {} assets to Case {}", assetIds.size(), caseId);

//        List<MediaAsset> assets = assetRepository.findAllByAssetIdIn(assetIds);
//        assets.forEach(asset -> {
//            asset.setCaseId(caseId);
//            asset.setStatus(AssetStatus.LINKED);
//        });
//
//        assetRepository.saveAll(assets);
        if (assetIds == null || assetIds.isEmpty()) return;

        var assets = assetRepository.findAllByAssetIdIn(assetIds);
        assets.forEach(asset -> {
            asset.setCaseId(caseId);
            asset.setStatus(AssetStatus.LINKED);
        });
        assetRepository.saveAll(assets);

        log.info("Hand-off Complete: Linked {} assets to case {}", assetIds.size(), caseId);

    }

    /**
     * CLEANUP TASK: Prevents binary storage leakage.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily 2 AM
    public void deleteExpiredPendingAssets() {
        Instant cutoff = Instant.now().minusSeconds(86400); // 24 hours
        assetRepository.findAllByStatusAndCreatedAtBefore(AssetStatus.PENDING, cutoff).forEach(asset -> {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(asset.getGridFsId())));
            assetRepository.delete(asset);
        });
    }

    @Override
    public void verifyAssetsExist(List<String> assetIds) {
//        long found = assetRepository.countByAssetIdInAndStatus(assetIds, AssetStatus.PENDING);
//        if (found != assetIds.size()) {
//            throw new IllegalArgumentException("One or more asset IDs are invalid or already processed");
//        }
        if (assetIds == null || assetIds.isEmpty()) return;

        long count = assetRepository.countByAssetIdInAndStatus(assetIds, AssetStatus.PENDING);
        if (count != assetIds.size()) {
            throw new IllegalArgumentException("Validation Error: Some uploaded assets are missing or already processed.");
        }
    }

    // Fetches metadata for Dashboard visualization (Used by Module 4 Review)
    public List<MediaMetadataView> getAssetsByCase(String caseId) {
        return assetRepository.findAllByCaseId(caseId).stream()
                .map(a -> new MediaMetadataView(a.getAssetId(), a.getFileName(),
                        a.getMimeType(), a.getFileSize(), "/api/v1/assets/" + a.getAssetId() + "/raw"))
                .toList();
    }

    // Streams raw content (Used by Browser to render photo/audio)
    public InputStreamResource streamAssetContent(String assetId) {
        MediaAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        return new InputStreamResource(storageProvider.retrieve(asset.getStoragePath()));
    }

    public void validateFile(String fileName, String mimeType, long size) {
        if (!ALLOWED_MIMES.contains(mimeType)) {
            throw new InvalidMediaTypeException("Unsupported format: " + mimeType);
        }
        if (size > MAX_FILE_SIZE) {
            throw new FileTooLargeException("Maximum upload size is 15MB");
        }
    }
}