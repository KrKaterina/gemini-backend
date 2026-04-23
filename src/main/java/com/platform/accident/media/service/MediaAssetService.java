package com.platform.accident.media.service;

import com.platform.accident.media.domain.*;
import com.platform.accident.media.exception.AssetNotFoundException;
import com.platform.accident.media.exception.FileTooLargeException;
import com.platform.accident.media.exception.InvalidMediaTypeException;
import com.platform.accident.media.infrastructure.StorageProvider;
import com.platform.accident.media.repository.MediaAssetRepository;
import com.platform.accident.review.exception.UnauthorizedReviewException;
import com.platform.accident.submission.integration.AiAssetData;
import com.platform.accident.submission.integration.AiMediaClient;
//import com.platform.accident.submission.integration.MediaAssetClient;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.media.MediaAssetClient;

import com.platform.integration.identity.IdentityContext;
import com.platform.integration.media.MediaMetadataView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.platform.accident.submission.integration.AiMediaClient;
import com.platform.accident.submission.integration.AiMediaResource;

import com.platform.integration.media.MediaAssetClient;
import com.platform.integration.media.MediaMetadataView;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAssetService implements MediaAssetClient, AiMediaClient {

    private final MediaAssetRepository assetRepository;
    private final StorageProvider storageProvider;

    private static final List<String> ALLOWED_MIMES = Arrays.asList("image/jpeg", "image/png", "audio/mpeg", "audio/wav");
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB limit

    private final GridFsTemplate gridFsTemplate;

    private final IdentityClient identityClient; // ΠΡΟΣΘΗΚΗ

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
   /* public String storePendingAsset(InputStream stream, String name, String type, long size) {
        String assetId = UUID.randomUUID().toString();
        var fileId = gridFsTemplate.store(stream, name, type);

        assetRepository.save(MediaAsset.builder()
                .assetId(assetId).gridFsId(fileId.toString())
                .fileName(name).mimeType(type).fileSize(size)
                .status(AssetStatus.PENDING).createdAt(Instant.now()).build());
        return assetId;
    }*/
    public String storePendingAsset(InputStream stream, String name, String type, long size, String ownerId) {
        String assetId = UUID.randomUUID().toString();
        var fileId = gridFsTemplate.store(stream, name, type);

        assetRepository.save(MediaAsset.builder()
                .assetId(assetId)
                .gridFsId(fileId.toString())
                .ownerId(ownerId) // Αποθηκεύουμε τον ιδιοκτήτη!
                .fileName(name)
                .mimeType(type)
                .fileSize(size)
                .status(AssetStatus.PENDING)
                .createdAt(Instant.now())
                .build());
        return assetId;
    }

    /**
     * Implementation of MediaAssetClient Port.
     * Links temporary uploads to the final Accident Report case.
     */
    @Override
//    public void linkAssetsToCase(String caseId, List<String> assetIds) {
//        log.info("Linking {} assets to Case {}", assetIds.size(), caseId);
//
////        List<MediaAsset> assets = assetRepository.findAllByAssetIdIn(assetIds);
////        assets.forEach(asset -> {
////            asset.setCaseId(caseId);
////            asset.setStatus(AssetStatus.LINKED);
////        });
////
////        assetRepository.saveAll(assets);
//        if (assetIds == null || assetIds.isEmpty()) return;
//
//        var assets = assetRepository.findAllByAssetIdIn(assetIds);
//        assets.forEach(asset -> {
//            asset.setCaseId(caseId);
//            asset.setStatus(AssetStatus.LINKED);
//        });
//        assetRepository.saveAll(assets);
//
//        log.info("Hand-off Complete: Linked {} assets to case {}", assetIds.size(), caseId);
//
//    }
    public void linkAssetsToCase(String caseId, List<String> assetIds) {
        //List<MediaAsset> assets = assetRepository.findAllByAssetIdIn(assetIds);
        var assets = assetRepository.findAllByAssetIdIn(assetIds);
        assets.forEach(asset -> {
            asset.setCaseId(caseId);
            asset.setStatus(com.platform.accident.media.domain.AssetStatus.LINKED);
        });
        assetRepository.saveAll(assets);
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

//    @Override
//    public byte[] getAssetBytes(String assetId) {
//        return new byte[0];
//    }

    // Fetches metadata for Dashboard visualization (Used by Module 4 Review)
//    public List<MediaMetadataView> getAssetsByCase(String caseId) {
//        return assetRepository.findAllByCaseId(caseId).stream()
//                .map(a -> new MediaMetadataView(a.getAssetId(), a.getFileName(),
//                        a.getMimeType(), a.getFileSize(), "/api/v1/assets/" + a.getAssetId() + "/raw"))
//                .toList();
//    }
    /**
     * ALIGNED PATHING: Synchronized with MediaController @GetMapping.
     */
    public List<MediaMetadataView> getAssetsByCase(String caseId) {
        return assetRepository.findAllByCaseId(caseId).stream()
                .map(a -> new MediaMetadataView(
                        a.getAssetId(),
                        a.getFileName(),
                        a.getMimeType(),
                        a.getFileSize(),
                        "/api/v1/media/" + a.getAssetId() + "/stream" // Πρέπει να ταιριάζει με τον Controller
                ))
                .toList();
    }

    // Streams raw content (Used by Browser to render photo/audio)
    public InputStreamResource streamAssetContent(String assetId) {
        MediaAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        // Χρησιμοποιούμε ObjectId εδώ για την αναζήτηση
        var file = gridFsTemplate.findOne(new Query(
                Criteria.where("_id").is(new org.bson.types.ObjectId(asset.getGridFsId()))
        ));

        if (file == null) throw new AssetNotFoundException("Physical file missing");

        try {
            return new InputStreamResource(gridFsTemplate.getResource(file).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Streaming failed", e);
        }
    }

    public MediaAsset getInternalMetadata(String assetId) {
        return assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));
    }

    /**
     * AI-VISION PORT: Internal byte-fetching for the LLM.
     */
    // @Override
//    public byte[] getAssetBytes(String assetId) {
//        log.info("Media Module: Providing bytes for AI analysis of asset {}", assetId);
//
//        var asset = assetRepository.findByAssetId(assetId)
//                .orElseThrow(() -> new RuntimeException("Asset not found for AI: " + assetId));
//
//        try (InputStream is = gridFsTemplate.getResource(
//                gridFsTemplate.findOne(new Query(Criteria.where("_id").is(asset.getGridFsId())))).getInputStream()) {
//
//            return is.readAllBytes();
//        } catch (Exception e) {
//            log.error("Failed to read bytes for AI Vision task", e);
//            throw new RuntimeException("AI binary fetch failed", e);
//        }
//    }


    /**
     * AI-VISION & AUDIO PORT: Υλοποίηση για το Intelligence Module.
     */
    public AiAssetData getAssetData(String assetId) {
        var asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));

        try (var is = gridFsTemplate.getResource(
                gridFsTemplate.findOne(org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(asset.getGridFsId())))
        ).getInputStream()) {

            byte[] bytes = is.readAllBytes();
            return new AiAssetData(bytes, asset.getMimeType()); // Επιστρέφουμε και το MimeType!

        } catch (Exception e) {
            log.error("Failed to fetch binary for AI: {}", assetId);
            throw new RuntimeException("Binary fetch failed", e);
        }
    }

    @Override
    //μονο για νεο τεστ μπηκε για το νεο
//    public AiMediaResource getAssetResource(String assetId) {
//        var asset = assetRepository.findByAssetId(assetId)
//                .orElseThrow(() -> new RuntimeException("Asset not found: " + assetId));
//
//        var gridFsFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(asset.getGridFsId())));
//
//        try {
//            return new AiMediaResource(
//                    gridFsTemplate.getResource(gridFsFile).getInputStream(),
//                    asset.getFileName(),
//                    asset.getMimeType(),
//                    asset.getFileSize()
//            );
//        } catch (Exception e) {
//            throw new RuntimeException("GridFS access error", e);
//        }
//    }

    public AiMediaResource getAssetResource(String assetId) {
        var asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        var gridFsFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new org.bson.types.ObjectId(asset.getGridFsId()))));

        try {
            return new AiMediaResource(
                    gridFsTemplate.getResource(gridFsFile).getInputStream(),
                    asset.getFileName(),
                    asset.getMimeType(),
                    asset.getFileSize()
            );
        } catch (Exception e) {
            throw new RuntimeException("GridFS access error", e);
        }
    }

    /*public MediaAsset getAuthorizedAsset(String assetId, IdentityContext context) {
        MediaAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        // Logic: Agent can see all; Customer can only see their own
        boolean canViewAll = context.permissions().contains("ASSET_VIEW");
        boolean isOwner = asset.getOwnerId().equals(context.userId());

        if (!canViewAll && !isOwner) {
            identityClient.logSecurityEvent(context.userId(), "UNAUTHORIZED_MEDIA_ACCESS", assetId);
            throw new UnauthorizedReviewException("You do not have access to this asset.");
        }

        return asset;
    }*/
    public MediaAsset getAuthorizedAsset(String assetId, IdentityContext context) {
        MediaAsset asset = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        // CHECK 1: Είναι Agent; (Permission check αντί για Role string)
        boolean canViewAll = context.permissions().contains("ASSET_VIEW");

        // CHECK 2: Είναι ο ιδιοκτήτης; (Προσθήκη Null-safe check)
        // Χρησιμοποιούμε Objects.equals για να αποφύγουμε το NullPointerException αν το asset.getOwnerId() είναι null
        boolean isOwner = java.util.Objects.equals(asset.getOwnerId(), context.userId());

        if (!canViewAll && !isOwner) {
            identityClient.logSecurityEvent(context.userId(), "UNAUTHORIZED_MEDIA_ACCESS", "Asset: " + assetId);
            throw new UnauthorizedReviewException("You do not have access to this asset.");
        }

        return asset;
    }
}