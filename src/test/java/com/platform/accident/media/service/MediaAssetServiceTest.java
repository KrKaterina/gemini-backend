package com.platform.accident.media.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.platform.accident.media.domain.AssetStatus;
import com.platform.accident.media.domain.MediaAsset;
import com.platform.accident.media.exception.AssetNotFoundException;
import com.platform.accident.media.infrastructure.StorageProvider;
import com.platform.accident.media.repository.MediaAssetRepository;
import com.platform.accident.submission.integration.AiAssetData;
import com.platform.accident.submission.integration.AiMediaResource;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.media.MediaMetadataView;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceTest {

    @Mock private MediaAssetRepository assetRepository;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private IdentityClient identityClient;
    @Mock private StorageProvider storageProvider;

    @InjectMocks
    private MediaAssetService mediaService;

    @Test
    @DisplayName("Upload (Phase 1): Should store metadata via StorageProvider path")
    void uploadAsset_Success() {
        InputStream stream = new ByteArrayInputStream("content".getBytes());
        when(storageProvider.store(any(), anyString(), anyString())).thenReturn("/paths/storage/1");

        String assetId = mediaService.uploadAsset(stream, "test.jpg", "image/jpeg");

        assertThat(assetId).isNotBlank();
        verify(assetRepository).save(any(MediaAsset.class));
    }

    @Test
    @DisplayName("Store Pending (GridFS): Should store and save owner metadata")
    void storePendingAsset_Success() {
        InputStream stream = new ByteArrayInputStream("content".getBytes());
        ObjectId gridFsId = new ObjectId();
        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(gridFsId);

        String assetId = mediaService.storePendingAsset(stream, "f.png", "image/png", 500L, "user1");

        assertThat(assetId).isNotBlank();
        verify(assetRepository).save(argThat(a ->
                a.getOwnerId().equals("user1") && a.getGridFsId().equals(gridFsId.toString())));
    }

    @Test
    @DisplayName("Get Assets By Case: Map metadata to View (bypassing method names)")
    void getAssetsByCase_MappingTest() {
        String testAssetId = "id-123";
        MediaAsset asset = MediaAsset.builder()
                .assetId(testAssetId)
                .fileName("test.jpg")
                .mimeType("image/jpeg")
                .build();
        when(assetRepository.findAllByCaseId("CASE1")).thenReturn(List.of(asset));

        List<MediaMetadataView> result = mediaService.getAssetsByCase("CASE1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).toString()).contains("/api/v1/media/" + testAssetId + "/stream");
    }

    @Test
    @DisplayName("Authorization: Allowed access if user is the owner")
    void getAuthorizedAsset_OwnerAccess() {
        String userId = "owner1";
        IdentityContext ctx = new IdentityContext(userId, "user", List.of(), List.of(), "REF", true);
        MediaAsset asset = MediaAsset.builder().assetId("a1").ownerId(userId).build();
        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));

        MediaAsset result = mediaService.getAuthorizedAsset("a1", ctx);

        assertThat(result).isEqualTo(asset);
        verify(identityClient).logSecurityEvent(eq(userId), eq("ASSET_VIEW_SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Authorization: Denied for strangers without ASSET_VIEW permission")
    void getAuthorizedAsset_StrangerDenied() {
        IdentityContext ctx = new IdentityContext("stranger", "user", List.of(), List.of(), "REF", true);
        MediaAsset asset = MediaAsset.builder().assetId("a1").ownerId("owner").build();
        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));
        when(identityClient.hasPermission("stranger", "ASSET_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> mediaService.getAuthorizedAsset("a1", ctx))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("AI Data: Should correctly fetch binary and mime type for AI analysis")
    void getAssetData_Success() throws IOException {
        String gridId = new ObjectId().toString();
        MediaAsset asset = MediaAsset.builder().gridFsId(gridId).mimeType("audio/wav").build();
        GridFSFile mockFile = mock(GridFSFile.class);
        GridFsResource mockRes = mock(GridFsResource.class);

        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));
        when(gridFsTemplate.findOne(any())).thenReturn(mockFile);
        when(gridFsTemplate.getResource(mockFile)).thenReturn(mockRes);
        when(mockRes.getInputStream()).thenReturn(new ByteArrayInputStream("dummy binary".getBytes()));

        AiAssetData data = mediaService.getAssetData("a1");

        assertThat(data.bytes()).isNotEmpty();
        assertThat(data.mimeType()).isEqualTo("audio/wav");
    }

    @Test
    @DisplayName("Cleanup Task: Should delete expired pending assets from both stores")
    void deleteExpiredPendingAssets_Flow() {
        MediaAsset oldAsset = MediaAsset.builder().gridFsId(new ObjectId().toString()).build();
        when(assetRepository.findAllByStatusAndCreatedAtBefore(eq(AssetStatus.PENDING), any()))
                .thenReturn(List.of(oldAsset));

        mediaService.deleteExpiredPendingAssets();

        verify(gridFsTemplate).delete(any(Query.class));
        verify(assetRepository).delete(oldAsset);
    }

    @Test
    @DisplayName("Streaming: Throws AssetNotFound if GridFS record is missing")
    void streamAssetContent_GridFsFailure() {
        MediaAsset asset = MediaAsset.builder().gridFsId(new ObjectId().toString()).build();
        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));
        when(gridFsTemplate.findOne(any())).thenReturn(null);

        assertThatThrownBy(() -> mediaService.streamAssetContent("a1"))
                .isInstanceOf(AssetNotFoundException.class)
                .hasMessageContaining("Physical file missing");
    }

    @Test
    @DisplayName("Linking: Updates Asset status and CaseID association")
    void linkAssetsToCase_Success() {
        MediaAsset asset = MediaAsset.builder().assetId("id1").status(AssetStatus.PENDING).build();
        when(assetRepository.findAllByAssetIdIn(anyList())).thenReturn(List.of(asset));

        mediaService.linkAssetsToCase("CASE-X", List.of("id1"));

        assertThat(asset.getStatus()).isEqualTo(AssetStatus.LINKED);
        assertThat(asset.getCaseId()).isEqualTo("CASE-X");
        verify(assetRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Verification: Throws exception when missing metadata")
    void verifyAssetsExist_Fail() {
        List<String> ids = List.of("missing");
        when(assetRepository.countByAssetIdInAndStatus(ids, AssetStatus.PENDING)).thenReturn(0L);

        assertThatThrownBy(() -> mediaService.verifyAssetsExist(ids))
                .isInstanceOf(IllegalArgumentException.class);
    }
}