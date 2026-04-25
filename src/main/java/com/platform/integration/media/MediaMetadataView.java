package com.platform.integration.media;

public record MediaMetadataView(
        String assetId,
        String fileName,
        String mimeType,
        long fileSize,
        String downloadUrl // Direct link to the binary stream
) {}

