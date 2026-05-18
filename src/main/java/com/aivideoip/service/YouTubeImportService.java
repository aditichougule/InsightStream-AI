package com.aivideoip.service;

import com.aivideoip.dto.VideoDTO;
import com.aivideoip.entity.User;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.UserRepository;
import com.aivideoip.repository.VideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Service for importing videos from YouTube URLs.
 * Uses yt-dlp to fetch metadata and create video records.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeImportService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    @Value("${app.ytdlp.enabled:true}")
    private boolean ytdlpEnabled;

    @Value("${app.ytdlp.path:yt-dlp}")
    private String ytdlpPath;

    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?(youtube|youtu|youtube-nocookie|youtubeembedded|yt)\\.(com|be)/.*",
            Pattern.CASE_INSENSITIVE);

    /**
     * Import a video from YouTube URL and create database record.
     * Uses yt-dlp to extract metadata without downloading the full video.
     *
     * @param youtubeUrl YouTube video URL
     * @param title Custom title (optional, uses YouTube title if not provided)
     * @param description Custom description (optional)
     * @param userId ID of user importing the video
     * @return VideoDTO of created video
     * @throws IllegalArgumentException if URL is invalid or yt-dlp fails
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    public VideoDTO importFromYouTube(
            String youtubeUrl,
            String title,
            String description,
            Long userId) {

        log.info("Starting YouTube import - User: {}, URL: {}", userId, youtubeUrl);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User not found with id: " + userId);
                });

        // Validate URL format
        validateYouTubeUrl(youtubeUrl);

        // Check if yt-dlp is available
        if (!ytdlpEnabled) {
            log.warn("yt-dlp is disabled in configuration");
            throw new RuntimeException("YouTube import is currently disabled");
        }

        // Fetch metadata from YouTube
        YouTubeMetadata metadata = extractMetadataFromYouTube(youtubeUrl);
        
        log.info("YouTube metadata extracted - VideoID: {}, Duration: {}s",
                metadata.videoId, metadata.durationSeconds);

        // Create video record with fetched metadata
        Video newVideo = Video.builder()
                .title(title != null ? title : metadata.title)
                .description(description != null ? description : metadata.description)
                .sourceUrl(youtubeUrl)
                .source(Video.VideoSource.YOUTUBE)
                .owner(user)
                .processingStatus(Video.ProcessingStatus.DOWNLOADING)
                .durationSeconds(metadata.durationSeconds)
                .thumbnailUrl(metadata.thumbnailUrl)
                .videoId(metadata.videoId)
                .build();

        Video savedVideo = videoRepository.save(newVideo);
        log.info("YouTube video record created - ID: {}, VideoID: {}", 
                savedVideo.getId(), metadata.videoId);

        // TODO: Queue async task for actual video download and processing
        // This will be handled by a background worker in the next phase

        return videoService.convertToDTO(savedVideo);
    }

    /**
     * Validate that the provided URL is a valid YouTube URL.
     */
    private void validateYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("YouTube URL cannot be empty");
        }

        if (!YOUTUBE_URL_PATTERN.matcher(url).matches()) {
            throw new IllegalArgumentException("Invalid YouTube URL format");
        }
    }

    /**
     * Extract metadata from YouTube using yt-dlp.
     * Runs yt-dlp with --dump-json to get video information without downloading.
     */
    private YouTubeMetadata extractMetadataFromYouTube(String youtubeUrl) {
        try {
            // Build yt-dlp command to extract JSON metadata
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ytdlpPath,
                    "--dump-json",
                    "--no-warnings",
                    youtubeUrl
            );

            Process process = processBuilder.start();
            
            // Read JSON output from yt-dlp
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            StringBuilder jsonOutput = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonOutput.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                log.error("yt-dlp error: {}", errorOutput.toString());
                throw new RuntimeException(
                        "Failed to fetch YouTube metadata: " + errorOutput.toString());
            }

            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(jsonOutput.toString());
            
            return YouTubeMetadata.builder()
                    .videoId(jsonNode.get("id").asText())
                    .title(jsonNode.get("title").asText())
                    .description(jsonNode.get("description").asText(""))
                    .durationSeconds(jsonNode.get("duration").asLong(0L))
                    .thumbnailUrl(jsonNode.get("thumbnail").asText(""))
                    .build();

        } catch (Exception e) {
            log.error("Failed to extract YouTube metadata: {}", e.getMessage());
            throw new RuntimeException(
                    "Failed to fetch YouTube metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Data class to hold YouTube video metadata.
     */
    @lombok.Data
    @lombok.Builder
    private static class YouTubeMetadata {
        private String videoId;
        private String title;
        private String description;
        private Long durationSeconds;
        private String thumbnailUrl;
    }
}
