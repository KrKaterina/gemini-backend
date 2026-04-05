package com.platform.accident.media.api;

import com.platform.accident.media.service.MediaAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class MediaAssetController {

    private final MediaAssetService mediaService;
    private final GridFsTemplate gridFsTemplate;

    /**
     * Entry point for mobile/web upload.
     * Happens BEFORE the accident report form is submitted.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(mediaService.storePendingAsset(
                file.getInputStream(), file.getOriginalFilename(), file.getContentType(), file.getSize()));
    }

    @GetMapping("/{assetId}/stream")
    public ResponseEntity<InputStreamResource> download(@PathVariable String assetId) throws Exception {
        var metadata = mediaService.getInternalMetadata(assetId); // Service fetch
        var file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(metadata.getGridFsId())));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                .body(new InputStreamResource(gridFsTemplate.getResource(file).getInputStream()));
    }
}