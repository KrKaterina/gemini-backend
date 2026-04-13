package com.platform.accident.media.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "media_assets")
public class MediaAsset {
    @Id
    private String assetId; // UUID generated at upload

    @Indexed
    private String caseId; // Reference to the Accident Report

    private String fileName;
    private String mimeType;
    private long fileSize;
    private AssetStatus status;
    private Instant createdAt;

    // Metadata for the storage backend
    private String storageProvider; // "GRIDFS" or "S3"
    private String storagePath;     // ID in GridFS or Key in S3
    private String gridFsId; // Physical reference in fs.files
}
