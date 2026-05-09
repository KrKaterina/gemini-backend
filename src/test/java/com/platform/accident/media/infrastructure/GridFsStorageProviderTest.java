package com.platform.accident.media.infrastructure;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    class GridFsStorageProviderTest {

        @Mock
        private GridFsTemplate gridFsTemplate;

        @Mock
        private GridFsResource mockResource;

        @InjectMocks
        private GridFsStorageProvider storageProvider;

        @Test
        @DisplayName("Store: Should correctly invoke GridFS store and return the ID as string")
        void store_Success() {
            InputStream stream = new ByteArrayInputStream("data".getBytes());
            ObjectId fakeId = new ObjectId();

            when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString()))
                    .thenReturn(fakeId);

            String result = storageProvider.store(stream, "file.png", "image/png");

            assertThat(result).isEqualTo(fakeId.toString());
            verify(gridFsTemplate).store(stream, "file.png", "image/png");
        }

        @Test
        @DisplayName("Delete: Should invoke delete with correct ID criteria")
        void delete_Success() {
            String path = "id-to-delete";

            storageProvider.delete(path);

            verify(gridFsTemplate).delete(any(Query.class));
        }

        @Test
        @DisplayName("Retrieve: Should successfully fetch stream from GridFsResource")
        void retrieve_Success() throws IOException {
            String path = new ObjectId().toString();
            InputStream expectedStream = new ByteArrayInputStream("content".getBytes());

            GridFSFile mockFile = mock(GridFSFile.class);

            when(gridFsTemplate.findOne(any(Query.class))).thenReturn(mockFile);
            when(gridFsTemplate.getResource(mockFile)).thenReturn(mockResource);
            when(mockResource.getInputStream()).thenReturn(expectedStream);

            InputStream result = storageProvider.retrieve(path);

            assertThat(result).isEqualTo(expectedStream);
            verify(gridFsTemplate).findOne(any(Query.class));
        }

        @Test
        @DisplayName("Retrieve Failure: Should wrap IOException into RuntimeException for the catch block")
        void retrieve_ExceptionCoverage() throws IOException {
            GridFSFile mockFile = mock(GridFSFile.class);

            when(gridFsTemplate.findOne(any(Query.class))).thenReturn(mockFile);
            when(gridFsTemplate.getResource(mockFile)).thenReturn(mockResource);
            when(mockResource.getInputStream()).thenThrow(new IOException("Disk read error"));

            assertThatThrownBy(() -> storageProvider.retrieve("some-id"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to stream asset from GridFS");
        }
    }


