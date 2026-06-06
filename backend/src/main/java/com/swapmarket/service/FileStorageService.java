package com.swapmarket.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.url-prefix}")
    private String urlPrefix;

    public String saveFile(MultipartFile file) throws IOException {
        LocalDate today = LocalDate.now();
        String datePath = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        Path targetDir = Paths.get(uploadPath, datePath);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        
        String newFilename = UUID.randomUUID().toString() + extension;
        Path targetPath = targetDir.resolve(newFilename);
        
        Files.copy(file.getInputStream(), targetPath);
        
        return urlPrefix + "/" + datePath + "/" + newFilename;
    }

    public Path getFilePath(String fileUrl) {
        String relativePath = fileUrl.substring(urlPrefix.length() + 1);
        return Paths.get(uploadPath, relativePath);
    }
}
