package com.platform.accident.media.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Component;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class GridFsStorageProvider implements StorageProvider {

    private final GridFsTemplate gridFsTemplate;

    @Override
    public String store(InputStream inputStream, String fileName, String mimeType) {
        return gridFsTemplate.store(inputStream, fileName, mimeType).toString();
    }

    @Override
    public InputStream retrieve(String storagePath) {
        GridFsResource resource = gridFsTemplate.getResource(
                gridFsTemplate.findOne(new Query(Criteria.where("_id").is(storagePath)))
        );
        try {
            return resource.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stream asset from GridFS", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        gridFsTemplate.delete(new Query(Criteria.where("_id").is(storagePath)));
    }
}
