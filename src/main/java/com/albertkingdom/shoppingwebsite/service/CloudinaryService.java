package com.albertkingdom.shoppingwebsite.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class CloudinaryService {
    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    private final Cloudinary cloudinaryConfig;
    private static String UPLOADED_FOLDER = "./upload/";

    public CloudinaryService(Cloudinary cloudinaryConfig) {
        this.cloudinaryConfig = cloudinaryConfig;
    }


    public Map uploadFile(Path tmpFilePath) throws IOException {
        try {
            File imgFile = tmpFilePath.toFile();
            Map uploadResult = cloudinaryConfig.uploader().upload(imgFile, ObjectUtils.asMap("folder", "shopping-website"));
            log.debug("cloudinary upload publicId={}", uploadResult.get("public_id"));
            imgFile.delete();
            return uploadResult;
        } catch (Exception e) {
            log.error("cloudinary upload failed", e);
            throw new IOException(e);
        }
    }

    public Map deleteFile(String publicId) throws IOException{
        try {
            Map deleteResult = cloudinaryConfig.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.debug("cloudinary delete publicId={} result={}", publicId, deleteResult.get("result"));
            return deleteResult;
        } catch (IOException e) {
            throw new IOException(e);
        }

    }

    public Path saveUploadedFiles(MultipartFile file) throws IOException {

        Path newPath = Files.createDirectories(Paths.get(UPLOADED_FOLDER)); //create directory

        byte[] bytes = file.getBytes();
        Path tempFilePath = Paths.get(UPLOADED_FOLDER + file.getOriginalFilename()); // full path with file name
        Files.write(tempFilePath, bytes); // write to file

        return tempFilePath;
    }
}
