package com.aivideoip.service;

import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Service for extracting audio from video files using FFmpeg.
 * Handles audio extraction, format conversion, and error handling.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioExtractionService {

    private final VideoRepository videoRepository;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffmpeg.enabled:true}")
    private boolean ffmpegEnabled;

    @Value("${app.audio.output-dir:uploads/audio}")
    private String audioOutputDir;

    @Value("${app.audio.format:mp3}")
    private String audioFormat;

    @Value("${app.audio.bitrate:128k}")
    private String audioBitrate;

    /**
     * Extract audio from a video file.
     * Converts video to MP3 (or specified format) using FFmpeg.
     *
     * @param videoId ID of the video to extract audio from
     * @return Path to the extracted audio file
     * @throws ResourceNotFoundException if video not found
     * @throws RuntimeException if audio extraction fails
     */
    @Transactional
    public String extractAudioFromVideo(Long videoId) {
        log.info("Starting audio extraction for video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", videoId));

        if (!ffmpegEnabled) {
            log.warn("FFmpeg is disabled in configuration");
            throw new RuntimeException("Audio extraction is currently disabled");
        }

        // Ensure input file exists
        if (video.getFilePath() == null && video.getSourceUrl() == null) {
            throw new IllegalArgumentException(
                    "Video must have either a file path or source URL for audio extraction");
        }

        // For uploaded files, use the file path
        String inputPath = video.getFilePath();
        if (inputPath == null) {
            // For YouTube or other sources, would need download first
            log.warn("Audio extraction for non-file sources not yet implemented: {}", videoId);
            throw new RuntimeException(
                    "Audio extraction for " + video.getSource() + " sources requires video download first");
        }

        // Ensure output directory exists
        ensureAudioDirectoryExists();

        // Generate output filename
        String audioFilename = generateAudioFilename(video);

        try {
            Path outputPath = Paths.get(audioOutputDir, audioFilename);
            extractAudio(inputPath, outputPath.toString());

            // Update video record with audio file path
            video.setAudioFilePath(outputPath.toString());
            videoRepository.save(video);

            log.info("Audio extracted successfully - VideoID: {}, OutputPath: {}",
                    videoId, outputPath.toAbsolutePath());

            return outputPath.toString();

        } catch (Exception e) {
            log.error("Failed to extract audio from video {}: {}", videoId, e.getMessage(), e);
            throw new RuntimeException("Failed to extract audio: " + e.getMessage(), e);
        }
    }

    /**
     * Extract audio from a video file using FFmpeg.
     * Executes: ffmpeg -i input.mp4 -q:a 0 -map a output.mp3
     */
    private void extractAudio(String inputPath, String outputPath) throws Exception {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("Input video file not found: " + inputPath);
        }

        try {
            // Build FFmpeg command
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-i", inputPath,
                    "-q:a", "0",
                    "-map", "a",
                    "-b:a", audioBitrate,
                    "-y",  // Overwrite output file if exists
                    outputPath
            );

            log.debug("Executing FFmpeg: {}", String.join(" ", processBuilder.command()));

            Process process = processBuilder.start();

            // Capture FFmpeg output for logging
            captureProcessOutput(process, inputPath);

            // Wait for process to complete with timeout
            boolean completed = process.waitFor(10, TimeUnit.MINUTES);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("FFmpeg process timed out after 10 minutes");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException(
                        String.format("FFmpeg failed with exit code %d for file: %s", exitCode, inputPath));
            }

            // Verify output file was created
            File outputFile = new File(outputPath);
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("FFmpeg produced no output or empty file");
            }

            log.info("Audio file created successfully: {} (Size: {} bytes)",
                    outputPath, outputFile.length());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Audio extraction interrupted", e);
        }
    }

    /**
     * Capture and log FFmpeg process output.
     * FFmpeg writes progress to stderr, so we monitor both stdout and stderr.
     */
    private void captureProcessOutput(Process process, String inputFile) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {

                String line;
                int updateCount = 0;
                while ((line = reader.readLine()) != null) {
                    // Log progress updates less frequently to reduce spam
                    if (line.contains("Duration") && updateCount++ % 10 == 0) {
                        log.debug("FFmpeg progress: {}", line.substring(0, Math.min(100, line.length())));
                    }
                }
            } catch (Exception e) {
                log.debug("Error reading FFmpeg output: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * Create audio output directory if it doesn't exist.
     */
    private void ensureAudioDirectoryExists() {
        try {
            Path directory = Paths.get(audioOutputDir);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                log.info("Created audio output directory: {}", directory.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create audio directory: {}", e.getMessage());
            throw new RuntimeException("Failed to create audio directory", e);
        }
    }

    /**
     * Generate a unique filename for the extracted audio.
     */
    private String generateAudioFilename(Video video) {
        String baseFilename = video.getFileName() != null
                ? video.getFileName().replaceAll("\\.[^.]+$", "")
                : "video_" + video.getId();

        return String.format("%s_%d.%s", baseFilename, System.nanoTime(), audioFormat);
    }

    /**
     * Check if FFmpeg is available on the system.
     */
    public boolean isFfmpegAvailable() {
        if (!ffmpegEnabled) {
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process process = processBuilder.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get the duration of a video file in seconds.
     * Useful for progress tracking and validation.
     */
    public long getVideoDuration(String filePath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    ffmpegPath,
                    "-i", filePath
            );

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            process.waitFor();

            // Parse duration from FFmpeg output
            // Format: Duration: HH:MM:SS.ms
            String durationStr = output.toString();
            if (durationStr.contains("Duration:")) {
                String[] parts = durationStr.split("Duration: ")[1].split(",")[0].split(":");
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2].split("\\.")[0]);

                return hours * 3600 + minutes * 60 + seconds;
            }

            return 0;

        } catch (Exception e) {
            log.warn("Failed to get video duration: {}", e.getMessage());
            return 0;
        }
    }
}
