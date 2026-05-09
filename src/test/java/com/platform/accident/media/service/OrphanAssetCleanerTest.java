package com.platform.accident.media.service;

import com.platform.accident.media.domain.AssetStatus;
import com.platform.accident.media.domain.MediaAsset;
import com.platform.accident.media.infrastructure.StorageProvider;
import com.platform.accident.media.repository.MediaAssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrphanAssetCleanerTest {

    @Mock
    private MediaAssetRepository repository;

    @Mock
    private StorageProvider storageProvider;

    @InjectMocks
    private OrphanAssetCleaner orphanAssetCleaner;

    @Test
    @DisplayName("Cleanup: Should delete both binary and metadata for each orphan found")
    void cleanupOrphans_WhenOrphansExist_ShouldDeleteThem() {
        MediaAsset orphan1 = MediaAsset.builder()
                .assetId("id1")
                .storagePath("path/1")
                .status(AssetStatus.PENDING)
                .build();

        MediaAsset orphan2 = MediaAsset.builder()
                .assetId("id2")
                .storagePath("path/2")
                .status(AssetStatus.PENDING)
                .build();

        when(repository.findAllByStatusAndCreatedAtBefore(eq(AssetStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(orphan1, orphan2));

        orphanAssetCleaner.cleanupOrphans();

        verify(storageProvider).delete("path/1");
        verify(storageProvider).delete("path/2");

        verify(repository).delete(orphan1);
        verify(repository).delete(orphan2);

        verify(repository, times(2)).delete(any());
    }

    @Test
    @DisplayName("Cleanup: Should do nothing if no orphan assets are found")
    void cleanupOrphans_WhenNoOrphans_ShouldInvokeNoDeletions() {
        when(repository.findAllByStatusAndCreatedAtBefore(eq(AssetStatus.PENDING), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        orphanAssetCleaner.cleanupOrphans();

        verifyNoInteractions(storageProvider);
        verify(repository, never()).delete(any());
    }
}