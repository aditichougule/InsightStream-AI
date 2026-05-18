package com.aivideoip.service;

import com.aivideoip.dto.VideoDTO;
import com.aivideoip.entity.User;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.UserRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for handling video file uploads and local storage.
 * Manages multipart file uploads and creates database records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VideoUploadService {
    
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoService videoService;
    
    @Value("${app.upload.video-dir:uploads/videos}")
    private String videoUploadDir;
    
    @Value("${app.upload.max-file-size:5368709120}")
    private Long maxFileSize;

    /**
     * Upload a video file and save it to disk along with database record.
     *
     * @param file Video file to upload
     * @param title Video title
     * @param description Video description
     * @param userId ID of user uploading the video
     * @return VideoDTO of created video
     * @throws IllegalArgumentException if file validation fails
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public VideoDTO uploadVideoFile(
            MultipartFile file,
            String title,
            String description,
            Long userId) {
        
        log.info("Processing upload - User: {}, File: {}, Size: {} MB",
                userId, file.getOriginalFilename(), file.getSize() / (1024 * 1024));
        
        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });
        
        // Validate file before processing
        validateUploadedFile(file);
        
        // Ensure upload directory exists
        ensureUploadDirectoryExists();
        
        // Generate unique filename to prevent conflicts
        String uniqueFilename = generateUniqueFilename(file.getOriginalFilename());
        
        try {
            // Save file to disk
            Path uploadPath = Paths.get(videoUploadDir, uniqueFilename);
            Files.createDirectories(uploadPath.getParent());
            Files.write(uploadPath, file.getBytes());
            
            log.info("File saved successfully - Path: {}, Size: {} MB",
                    uploadPath.toAbsolutePath(), file.getSize() / (1024 * 1024));
            
            // Create video record in database
            Video newVideo = Video.builder()
                    .title(title)
                    .description(description)
                    .sourceUrl("file://" + uploadPath.toAbsolutePath())
                    .source(Video.VideoSource.UPLOADED)
                    .owner(user)
                    .processingStatus(Video.ProcessingStatus.PENDING)
                    .fileName(uniqueFilename)
                    .filePath(uploadPath.toAbsolutePath().toString())
                    .fileSize(file.getSize())
                    .build();
            
            Video savedVideo = videoRepository.save(newVideo);
            log.info("Video record created - ID: {}, Title: {}", savedVideo.getId(), title);
            
            return videoService.convertToDTO(savedVideo);
            
        } catch (IOException e) {
            log.error("Failed to save video file: {} - Error: {}", file.getOriginalFilename(), e.getMessage());
            throw new RuntimeException("Failed to save video file: " + e.getMessage(), e);
        }
    }

    /**
     * Validate that the uploaded file meets requirements.
     */
    private void validateUploadedFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (file.getSize() > maxFileSize) {
            long maxSizeGB = maxFileSize / (1024 * 1024 * 1024);
            throw new IllegalArgumentException(
                    String.format("File size exceeds limit of %d GB", maxSizeGB));
        }
        
        String contentType = file.getContentType();
        if (!isValidVideoContentType(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid video format. Supported formats: MP4, AVI, MKV, MOV, FLV");
        }
    }

    /**
     * Check if content type is a valid video format.
     */
    private boolean isValidVideoContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        return contentType.startsWith("video/") ||
                contentType.equals("application/x-matroska") ||
                contentType.equals("application/octet-stream");
    }

    /**
     * Create upload directory if it doesn't exist.
     */
    private void ensureUploadDirectoryExists() {
        try {
            Path directory = Paths.get(videoUploadDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Created upload directory: {}", directory.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", e.getMessage());
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    /**
     * Generate a unique filename using UUID to prevent file conflicts.
     */
    private String generateUniqueFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return uuid + extension;
    }
}
