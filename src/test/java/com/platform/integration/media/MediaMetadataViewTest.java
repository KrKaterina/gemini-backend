package com.platform.integration.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediaMetadataViewTest {

    @Test
    @DisplayName("Record Verification: Should correctly store and retrieve media metadata")
    void testMediaMetadataViewProperties() {
        String assetId = "asset-789";
        String fileName = "accident_photo.jpg";
        String mimeType = "image/jpeg";
        long fileSize = 512000L;
        String url = "/api/v1/media/asset-789/stream";

        MediaMetadataView view = new MediaMetadataView(assetId, fileName, mimeType, fileSize, url);

        assertThat(view.assetId()).isEqualTo(assetId);
        assertThat(view.fileName()).isEqualTo(fileName);
        assertThat(view.mimeType()).isEqualTo(mimeType);
        assertThat(view.fileSize()).isEqualTo(fileSize);
        assertThat(view.downloadUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("Record Equality: Verifies that equals and hashCode work correctly for identical data")
    void testEquality() {
        MediaMetadataView view1 = new MediaMetadataView("ID", "file", "mime", 100L, "/url");
        MediaMetadataView view2 = new MediaMetadataView("ID", "file", "mime", 100L, "/url");
        MediaMetadataView view3 = new MediaMetadataView("DIFF", "file", "mime", 100L, "/url");

        assertThat(view1).isEqualTo(view2);
        assertThat(view1).isNotEqualTo(view3);

        assertThat(view1.hashCode()).isEqualTo(view2.hashCode());
        assertThat(view1.hashCode()).isNotEqualTo(view3.hashCode());
    }

    @Test
    @DisplayName("Record ToString: Verifies the string representation for log coverage")
    void testToString() {
        MediaMetadataView view = new MediaMetadataView("ID", "test.jpg", "image/jpeg", 500, "/url");
        String result = view.toString();

        assertThat(result)
                .contains("assetId=ID")
                .contains("fileName=test.jpg")
                .contains("fileSize=500")
                .contains("downloadUrl=/url");
    }
}