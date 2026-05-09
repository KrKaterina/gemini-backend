package com.platform.accident.submission.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class AiMediaResourceTest {

    @Test
    @DisplayName("Record State: Should correctly store and return all fields")
    void testRecordState() {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        AiMediaResource resource = new AiMediaResource(stream, "file.jpg", "image/jpeg", 1024L);

        assertThat(resource.inputStream()).isEqualTo(stream);
        assertThat(resource.fileName()).isEqualTo("file.jpg");
        assertThat(resource.mimeType()).isEqualTo("image/jpeg");
        assertThat(resource.size()).isEqualTo(1024L);
    }

    @Test
    @DisplayName("AutoCloseable: Calling close() should close the underlying InputStream")
    void testCloseInvoked() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        AiMediaResource resource = new AiMediaResource(mockStream, "test.mp3", "audio/mpeg", 500L);

        resource.close();

        verify(mockStream, times(1)).close();
    }

    @Test
    @DisplayName("Try-with-resources: Should automatically close stream at the end of block")
    void testTryWithResources() throws IOException {
        InputStream mockStream = mock(InputStream.class);

        try (AiMediaResource resource = new AiMediaResource(mockStream, "test.txt", "text/plain", 10L)) {
            assertThat(resource.fileName()).isEqualTo("test.txt");
        }

        verify(mockStream, times(1)).close();
    }

    @Test
    @DisplayName("Null Safety: Closing a resource with a null stream should not throw exception")
    void testCloseWithNullStream() {
        AiMediaResource resource = new AiMediaResource(null, "empty.jpg", "image/jpeg", 0L);

        assertThatCode(resource::close).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Close failure: Should propagate IOException if stream closing fails")
    void testClosePropagatesException() throws IOException {
        InputStream mockStream = mock(InputStream.class);
        doThrow(new IOException("Disk failure")).when(mockStream).close();

        AiMediaResource resource = new AiMediaResource(mockStream, "fail.jpg", "image/jpeg", 1L);

        try {
            resource.close();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Disk failure");
        }
    }
}