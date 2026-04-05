package com.platform.accident.media.service;

import com.platform.accident.media.domain.AssetStatus;
import com.platform.accident.media.infrastructure.StorageProvider;
import com.platform.accident.media.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OrphanAssetCleaner {

    private final MediaAssetRepository repository;
    private final StorageProvider storageProvider;

    /**
     * Automatically deletes binary files that were uploaded
     * but never linked to an accident report within 24 hours.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Daily at 1:00 AM
    public void cleanupOrphans() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        repository.findAllByStatusAndCreatedAtBefore(AssetStatus.PENDING, cutoff)
                .forEach(asset -> {
                    storageProvider.delete(asset.getStoragePath());
                    repository.delete(asset);
                });
    }
}