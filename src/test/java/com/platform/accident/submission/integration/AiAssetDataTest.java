package com.platform.accident.submission.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAssetDataTest {

    @Test
    @DisplayName("Record Verification: Should correctly store and retrieve bytes and mime type")
    void testAiAssetDataProperties() {
        // Arrange
        byte[] expectedBytes = "dummy photo data".getBytes();
        String expectedMime = "image/jpeg";

        // Act
        AiAssetData data = new AiAssetData(expectedBytes, expectedMime);

        // Assert
        assertThat(data.bytes()).isEqualTo(expectedBytes);
        assertThat(data.mimeType()).isEqualTo(expectedMime);
    }

    @Test
    @DisplayName("Record Equality: Verifies equals, hashCode and toString for Coverage")
    void testEquality() {
        byte[] bytes = {1, 2, 3};
        String mime = "audio/mpeg";

        AiAssetData data1 = new AiAssetData(bytes, mime);
        AiAssetData data2 = new AiAssetData(bytes, mime);

        // Έλεγχος ισότητας (για το Coverage)
        assertThat(data1).isEqualTo(data2);
        assertThat(data1.hashCode()).isEqualTo(data2.hashCode());

        // Έλεγχος toString (για το Coverage)
        assertThat(data1.toString()).contains("mimeType=audio/mpeg");
    }
}