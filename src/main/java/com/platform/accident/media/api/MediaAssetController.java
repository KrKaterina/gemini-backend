package com.platform.accident.media.api;

import com.platform.accident.media.service.MediaAssetService;
import com.platform.integration.identity.IdentityContext;
import com.platform.integration.identity.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaAssetController {

    private final MediaAssetService mediaService;
    private final GridFsTemplate gridFsTemplate;

    /**
     * Entry point for mobile/web upload.
     * Happens BEFORE the accident report form is submitted.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) throws IOException {

        // DERIVE IDENTITY: Παίρνουμε το userId από το έμπιστο context
        IdentityContext ctx = SecurityContext.getRequired(request);

        // Περνάμε το ctx.userId() στη μέθοδο
        String assetId = mediaService.storePendingAsset(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                ctx.userId() // Προσθήκη παραμέτρου
        );
        return ResponseEntity.ok(Map.of("assetId", assetId));
    }

    @GetMapping("/{assetId}/stream")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String assetId,
            HttpServletRequest httpRequest) throws Exception {

        IdentityContext requester = SecurityContext.getRequired(httpRequest);

        // ENFORCE AUTHORIZATION: Check permission and ownership at the service layer
        var assetMetadata = mediaService.getAuthorizedAsset(assetId, requester);

        var file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(assetMetadata.getGridFsId())));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(assetMetadata.getMimeType()))
                .body(new InputStreamResource(gridFsTemplate.getResource(file).getInputStream()));
    }
}