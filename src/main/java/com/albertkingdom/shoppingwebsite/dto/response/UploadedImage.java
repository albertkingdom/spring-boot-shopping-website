package com.albertkingdom.shoppingwebsite.dto.response;

/**
 * The two Cloudinary result fields the app cares about after a successful
 * upload: the public URL used in product responses and the {@code public_id}
 * used to delete or replace the asset later.
 */
public class UploadedImage {
    private final String url;
    private final String publicId;

    public UploadedImage(String url, String publicId) {
        this.url = url;
        this.publicId = publicId;
    }

    public String getUrl() {
        return url;
    }

    public String getPublicId() {
        return publicId;
    }
}
