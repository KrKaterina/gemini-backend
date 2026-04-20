package com.platform.accident.submission.integration;

import java.io.InputStream;
import java.io.IOException;

/**
 * Added AutoCloseable to ensure InputStreams are closed after
 * uploading to the Google File API.
 */
public record AiMediaResource(
        InputStream inputStream,
        String fileName,
        String mimeType,
        long size
) implements AutoCloseable {

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}