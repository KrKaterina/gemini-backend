package com.platform.accident.media.infrastructure;

import java.io.InputStream;

/**
 * Strategy pattern for file storage backends.
 */
public interface StorageProvider {
    String store(InputStream inputStream, String fileName, String mimeType);
    InputStream retrieve(String storagePath);
    void delete(String storagePath);
}