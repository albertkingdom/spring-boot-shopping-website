package com.albertkingdom.shoppingwebsite.service;

import com.albertkingdom.shoppingwebsite.dto.response.UploadedImage;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryServiceTest {

    private static final byte[] PNG_MAGIC_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            // enough tail bytes to look like a real png header
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01
    };

    @Test
    void uploadImage_returnsUrlAndPublicId_andDeletesTempFile() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        Map<String, Object> result = new HashMap<>();
        result.put("url", "https://cdn.example.com/photo.png");
        result.put("public_id", "shopping-website/abc123");
        when(uploader.upload(any(File.class), any())).thenReturn(result);

        CloudinaryService service = new CloudinaryService(cloudinary, 5 * 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile(
                "productImage", "photo.png", "image/png", PNG_MAGIC_BYTES);

        UploadedImage uploaded = service.uploadImage(file);

        assertEquals("https://cdn.example.com/photo.png", uploaded.getUrl());
        assertEquals("shopping-website/abc123", uploaded.getPublicId());

        // The temp file we handed Cloudinary should be gone after upload.
        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        verify(uploader).upload(captor.capture(), any());
        assertFalse(captor.getValue().exists(), "temp file should be deleted after upload");
    }

    @Test
    void uploadImage_deletesTempFile_evenWhenCloudinaryThrows() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(File.class), any()))
                .thenThrow(new IOException("network down"));

        CloudinaryService service = new CloudinaryService(cloudinary, 5 * 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile(
                "productImage", "photo.png", "image/png", PNG_MAGIC_BYTES);

        assertThrows(IOException.class, () -> service.uploadImage(file));

        ArgumentCaptor<File> captor = ArgumentCaptor.forClass(File.class);
        verify(uploader).upload(captor.capture(), any());
        assertFalse(captor.getValue().exists(), "temp file should be deleted even on upload failure");
    }

    @Test
    void uploadImage_rejectsEmptyFile() {
        CloudinaryService service = new CloudinaryService(mock(Cloudinary.class), 5 * 1024 * 1024);
        MockMultipartFile file = new MockMultipartFile("productImage", "photo.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file));
    }

    @Test
    void uploadImage_rejectsOversizedFile() {
        CloudinaryService service = new CloudinaryService(mock(Cloudinary.class), 16); // 16 bytes cap
        MockMultipartFile file = new MockMultipartFile(
                "productImage", "photo.png", "image/png", PNG_MAGIC_BYTES);

        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file));
    }

    @Test
    void uploadImage_rejectsNonImageContentType() {
        CloudinaryService service = new CloudinaryService(mock(Cloudinary.class), 5 * 1024 * 1024);
        // Random bytes, no image magic; client-declared type is text.
        MockMultipartFile file = new MockMultipartFile(
                "productImage", "notes.txt", "text/plain", "hello world".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file));
    }
}
