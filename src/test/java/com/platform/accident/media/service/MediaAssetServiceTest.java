package com.platform.accident.media.service;

import com.platform.accident.media.domain.AssetStatus;
import com.platform.accident.media.domain.MediaAsset;
import com.platform.accident.media.exception.AssetNotFoundException;
import com.platform.accident.media.repository.MediaAssetRepository;
import com.platform.identity.exception.UnauthorizedException;
import com.platform.integration.identity.IdentityClient;
import com.platform.integration.identity.IdentityContext;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaAssetServiceTest {

    @Mock private MediaAssetRepository assetRepository;
    @Mock private GridFsTemplate gridFsTemplate;
    @Mock private IdentityClient identityClient;

    @InjectMocks
    private MediaAssetService mediaService;

    @Test
    @DisplayName("Upload: Should store binary in GridFS and metadata in Repository")
    void storePendingAsset_Success() {
        InputStream stream = new ByteArrayInputStream("dummy data".getBytes());
        ObjectId mockFileId = new ObjectId();
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(mockFileId);

        String assetId = mediaService.storePendingAsset(stream, "test.jpg", "image/jpeg", 1024L, "user1");

        assertThat(assetId).isNotBlank();
        verify(gridFsTemplate).store(eq(stream), eq("test.jpg"), eq("image/jpeg"));
        verify(assetRepository).save(argThat(asset ->
                asset.getOwnerId().equals("user1") &&
                        asset.getStatus() == AssetStatus.PENDING &&
                        asset.getGridFsId().equals(mockFileId.toString())
        ));
    }

    @Test
    @DisplayName("Authorization: Owner should be allowed to access their own asset")
    void getAuthorizedAsset_OwnerAccess_Allowed() {
        String ownerId = "owner_123";
        IdentityContext context = new IdentityContext(ownerId, "user", List.of("ROLE_CUSTOMER"), List.of(), "REF", true);
        MediaAsset asset = MediaAsset.builder().assetId("a1").ownerId(ownerId).build();

        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));

        MediaAsset result = mediaService.getAuthorizedAsset("a1", context);

        assertThat(result).isEqualTo(asset);
        verify(identityClient).logSecurityEvent(eq(ownerId), eq("ASSET_VIEW_SUCCESS"), anyString());
    }

    @Test
    @DisplayName("Authorization: Stranger should be denied access even if authenticated")
    void getAuthorizedAsset_StrangerAccess_Denied() {
        IdentityContext strangerContext = new IdentityContext("stranger_1", "user", List.of("ROLE_CUSTOMER"), List.of(), "REF", true);
        MediaAsset asset = MediaAsset.builder().assetId("a1").ownerId("real_owner").build();

        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));
        when(identityClient.hasPermission("stranger_1", "ASSET_VIEW")).thenReturn(false);

        assertThatThrownBy(() -> mediaService.getAuthorizedAsset("a1", strangerContext))
                .isInstanceOf(UnauthorizedException.class);

        verify(identityClient).logSecurityEvent(eq("stranger_1"), eq("UNAUTHORIZED_MEDIA_ACCESS"), anyString());
    }

    @Test
    @DisplayName("Authorization: Agent with specific permission should be allowed access")
    void getAuthorizedAsset_AgentAccess_Allowed() {
        IdentityContext agentContext = new IdentityContext("agent_1", "agent", List.of("ROLE_AGENT"), List.of("ASSET_VIEW"), "REF", true);
        MediaAsset asset = MediaAsset.builder().assetId("a1").ownerId("real_owner").build();

        when(assetRepository.findByAssetId("a1")).thenReturn(Optional.of(asset));
        when(identityClient.hasPermission("agent_1", "ASSET_VIEW")).thenReturn(true);

        MediaAsset result = mediaService.getAuthorizedAsset("a1", agentContext);

        assertThat(result).isEqualTo(asset);
    }

    @Test
    @DisplayName("Linking: Should change status from PENDING to LINKED and attach Case ID")
    void linkAssetsToCase_Success() {
        MediaAsset asset = MediaAsset.builder().assetId("a1").status(AssetStatus.PENDING).build();
        when(assetRepository.findAllByAssetIdIn(any())).thenReturn(List.of(asset));

        mediaService.linkAssetsToCase("CASE-001", List.of("a1"));


        assertThat(asset.getStatus()).isEqualTo(AssetStatus.LINKED);
        assertThat(asset.getCaseId()).isEqualTo("CASE-001");
        verify(assetRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Verification: Should throw exception if not all pending assets are found")
    void verifyAssetsExist_Fail() {
        List<String> ids = List.of("id1", "id2");
        when(assetRepository.countByAssetIdInAndStatus(ids, AssetStatus.PENDING)).thenReturn(1L);

        assertThatThrownBy(() -> mediaService.verifyAssetsExist(ids))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Validation Error");
    }
}