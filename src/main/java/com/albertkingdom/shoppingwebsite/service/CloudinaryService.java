package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.UploadedImage;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class CloudinaryService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    /** Content types we accept for product images. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/webp", "image/gif")));

    private final Cloudinary cloudinaryConfig;
    private final long maxUploadBytes;

    public CloudinaryService(Cloudinary cloudinaryConfig,
                             @Value("${app.upload.max-bytes:5242880}") long maxUploadBytes) {
        this.cloudinaryConfig = cloudinaryConfig;
        this.maxUploadBytes = maxUploadBytes;
    }

    /**
     * Validate and upload one product image to Cloudinary. The temp file is
     * created with a random name (no path-traversal risk from the user-supplied
     * filename) and is always deleted, whether the Cloudinary call succeeds or
     * throws.
     */
    public UploadedImage uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("uploaded file is empty");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException(
                    "uploaded file exceeds max size of " + maxUploadBytes + " bytes");
        }
        String detected = detectContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(detected)) {
            throw new IllegalArgumentException(
                    "unsupported image type: " + detected + " (allowed: " + ALLOWED_CONTENT_TYPES + ")");
        }

        Path tempFile = Files.createTempFile("shopping-upload-", suffixFor(detected));
        try {
            file.transferTo(tempFile.toFile());
            Map<?, ?> uploadResult = cloudinaryConfig.uploader().upload(
                    tempFile.toFile(),
                    ObjectUtils.asMap("folder", "shopping-website"));
            log.debug("cloudinary upload publicId={}", uploadResult.get("public_id"));
            return new UploadedImage(
                    String.valueOf(uploadResult.get("url")),
                    String.valueOf(uploadResult.get("public_id")));
        } catch (Exception e) {
            log.error("cloudinary upload failed", e);
            throw new IOException(e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanup) {
                log.warn("failed to delete temp upload file {}", tempFile, cleanup);
            }
        }
    }

    public void deleteFile(String publicId) throws IOException {
        if (publicId == null || publicId.isEmpty()) {
            return;
        }
        try {
            Map<?, ?> deleteResult = cloudinaryConfig.uploader()
                    .destroy(publicId, ObjectUtils.emptyMap());
            String result = String.valueOf(deleteResult.get("result"));
            log.debug("cloudinary delete publicId={} result={}", publicId, result);
            if (!"ok".equals(result) && !"not found".equals(result)) {
                // Cloudinary reports non-ok, non-missing states in the result field.
                // Log for the retry / compensation task to reconcile later; do not
                // fail the caller, since the DB row was already updated to point
                // away from this asset.
                log.warn("cloudinary delete returned unexpected result publicId={} result={}", publicId, result);
            }
        } catch (IOException e) {
            log.warn("cloudinary delete failed publicId={}; asset is orphaned pending reconciliation",
                    publicId, e);
            throw e;
        }
    }

    private static String detectContentType(MultipartFile file) throws IOException {
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            String detected = URLConnection.guessContentTypeFromStream(in);
            if (detected != null) {
                return detected;
            }
        }
        // Fall back to the client-supplied Content-Type if magic bytes did not
        // resolve. The client value is untrusted, so this only helps when the
        // format is recognized by the browser but not by URLConnection.
        String clientType = file.getContentType();
        return clientType == null ? "application/octet-stream" : clientType;
    }

    private static String suffixFor(String contentType) {
        switch (contentType) {
            case "image/jpeg": return ".jpg";
            case "image/png":  return ".png";
            case "image/webp": return ".webp";
            case "image/gif":  return ".gif";
            default:           return "";
        }
    }
}
