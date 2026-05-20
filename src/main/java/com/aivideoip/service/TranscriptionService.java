package com.aivideoip.service;

import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for calling the Whisper Python microservice to transcribe audio.
 * Handles communication with the standalone Python Flask service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptionService {

    private final TranscriptChunkRepository transcriptChunkRepository;
    private final VideoRepository videoRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.whisper.service-url:http://localhost:5000}")
    private String whisperServiceUrl;

    @Value("${app.whisper.enabled:true}")
    private boolean whisperEnabled;

    /**
     * Transcribe audio file using Whisper microservice.
     * Calls Python service and stores transcript chunks in database.
     *
     * @param videoId ID of the video to transcribe
     * @return List of transcript chunks
     * @throws ResourceNotFoundException if video not found
     * @throws RuntimeException if transcription fails
     */
    @Transactional
    public List<TranscriptChunkDTO> transcribeAudio(Long videoId) {
        log.info("Starting transcription for video: {}", videoId);

        // Validate Whisper service is enabled
        if (!whisperEnabled) {
            log.warn("Whisper service is disabled in configuration");
            throw new RuntimeException("Transcription service is currently disabled");
        }

        // Retrieve video
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> {
                    log.warn("Video not found: {}", videoId);
                    return new ResourceNotFoundException("Video not found with id: " + videoId);
                });

        // Validate audio file path exists
        String audioFilePath = video.getAudioFilePath();
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            throw new IllegalArgumentException("Audio file not extracted. Extract audio first.");
        }

        try {
            // Call Whisper microservice
            WhisperResponse whisperResponse = callWhisperService(audioFilePath);

            log.info("Transcription received - Segments: {}", whisperResponse.getSegments().size());

            // Delete existing transcripts for this video
            transcriptChunkRepository.deleteByVideoId(videoId);

            // Store transcript chunks in database
            List<TranscriptChunkDTO> chunks = new ArrayList<>();
            for (WhisperResponse.Segment segment : whisperResponse.getSegments()) {
                TranscriptChunk chunk = TranscriptChunk.builder()
                        .video(video)
                        .chunkText(segment.getText())
                        .startTime(segment.getStart())  // seconds
                        .endTime(segment.getEnd())      // seconds
                        .build();

                TranscriptChunk saved = transcriptChunkRepository.save(chunk);
                chunks.add(convertToDTO(saved));

                log.debug("Transcript chunk saved - ID: {}, Duration: {} - {}s",
                        saved.getId(), saved.getStartTime(), saved.getEndTime());
            }

            // Update video processing status
            video.setProcessingStatus(Video.ProcessingStatus.COMPLETED);
            videoRepository.save(video);

            log.info("Transcription completed - Video: {}, Chunks: {}",
                    videoId, chunks.size());

            return chunks;

        } catch (Exception e) {
            log.error("Transcription failed for video {}: {}", videoId, e.getMessage());
            
            // Update video with error status
            video.setProcessingStatus(Video.ProcessingStatus.FAILED);
            video.setErrorMessage("Transcription failed: " + e.getMessage());
            videoRepository.save(video);
            
            throw new RuntimeException("Failed to transcribe audio: " + e.getMessage(), e);
        }
    }

    /**
     * Call Whisper Python microservice to transcribe audio.
     */
    private WhisperResponse callWhisperService(String audioFilePath) throws Exception {
        log.debug("Calling Whisper service - AudioPath: {}", audioFilePath);

        String url = whisperServiceUrl + "/transcribe/file";

        // Build request payload
        WhisperRequest request = new WhisperRequest(audioFilePath, "en");

        try {
            // Call Whisper service
            String responseBody = restTemplate.postForObject(url, request, String.class);

            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);

            if (!responseJson.get("success").asBoolean()) {
                throw new RuntimeException(
                        "Whisper service error: " + responseJson.get("message").asText());
            }

            // Extract transcript data
            JsonNode dataNode = responseJson.get("data");
            WhisperResponse response = new WhisperResponse();

            // Parse segments
            List<WhisperResponse.Segment> segments = new ArrayList<>();
            for (JsonNode segmentNode : dataNode.get("segments")) {
                WhisperResponse.Segment segment = new WhisperResponse.Segment(
                        segmentNode.get("start").asInt(),
                        segmentNode.get("end").asInt(),
                        segmentNode.get("text").asText()
                );
                segments.add(segment);
            }

            response.setSegments(segments);
            response.setFullText(dataNode.get("full_text").asText());
            response.setLanguage(dataNode.get("language").asText());

            log.debug("Whisper response parsed - Segments: {}", segments.size());

            return response;

        } catch (Exception e) {
            log.error("Failed to call Whisper service: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to Whisper service: " + e.getMessage(), e);
        }
    }

    /**
     * Get all transcript chunks for a video.
     */
    public List<TranscriptChunkDTO> getVideoTranscripts(Long videoId) {
        log.debug("Fetching transcripts for video: {}", videoId);

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id: " + videoId));

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);

        return chunks.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Get full transcription text for a video.
     */
    public String getVideoTranscriptionText(Long videoId) {
        log.debug("Fetching full transcription for video: {}", videoId);

        List<TranscriptChunk> chunks = transcriptChunkRepository
                .findByVideoIdOrderByStartTime(videoId);

        return chunks.stream()
                .map(TranscriptChunk::getChunkText)
                .reduce((a, b) -> a + " " + b)
                .orElse("");
    }

    /**
     * Convert TranscriptChunk entity to DTO.
     */
    private TranscriptChunkDTO convertToDTO(TranscriptChunk chunk) {
        return TranscriptChunkDTO.builder()
                .id(chunk.getId())
                .videoId(chunk.getVideo().getId())
                .chunkText(chunk.getChunkText())
                .startTime(chunk.getStartTime())
                .endTime(chunk.getEndTime())
                .speaker(chunk.getSpeaker())
                .topic(chunk.getTopic())
                .build();
    }

    /**
     * Request payload for Whisper service.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WhisperRequest {
        private String file_path;
        private String language;
    }

    /**
     * Response from Whisper service.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WhisperResponse {
        private List<Segment> segments;
        private String fullText;
        private String language;

        @lombok.Data
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class Segment {
            private int start;
            private int end;
            private String text;
        }
    }
}
