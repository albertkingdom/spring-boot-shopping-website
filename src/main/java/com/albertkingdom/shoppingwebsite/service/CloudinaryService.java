package com.albertkingdom.shoppingwebsite.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private final Cloudinary cloudinaryConfig;
    private static String UPLOADED_FOLDER = "./upload/";

    public CloudinaryService(Cloudinary cloudinaryConfig) {
        this.cloudinaryConfig = cloudinaryConfig;
    }


    public Map uploadFile(Path tmpFilePath) throws IOException {
        try {
            File imgFile = tmpFilePath.toFile();
            Map uploadResult = cloudinaryConfig.uploader().upload(imgFile, ObjectUtils.asMap("folder", "shopping-website"));
            System.out.println("uploadResult " + uploadResult);
            imgFile.delete();
            return uploadResult;
        } catch (Exception e) {
            System.out.println(e);
            throw new IOException(e);
        }
    }

    public Map deleteFile(String publicId) throws IOException{
        try {
            Map deleteResult = cloudinaryConfig.uploader().destroy(publicId, ObjectUtils.emptyMap());
            System.out.println("deleteResult " + deleteResult);
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
