package com.aivideoip.service;

import com.aivideoip.dto.StructuredSummaryResponse;
import com.aivideoip.dto.TopicExtractionRequest;
import com.aivideoip.entity.Topic;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TopicRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for extracting topics and chapters from video transcripts
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TopicExtractionService {
    
    private final TopicRepository topicRepository;
    private final VideoRepository videoRepository;
    private final OllamaClient ollamaClient;
    
    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;
    
    @Value("${app.ollama.model:llama2}")
    private String model;
    
    /**
     * Extract topics from video transcript
     */
    public List<StructuredSummaryResponse.TopicData> extractTopics(TopicExtractionRequest request) {
        log.info("Extracting topics from video: {}", request.getVideoId());
        
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        
        String transcript = request.getTranscript();
        if (transcript == null || transcript.isBlank()) {
            log.warn("Empty transcript provided for video: {}", request.getVideoId());
            return List.of();
        }
        
        List<StructuredSummaryResponse.TopicData> topics = new ArrayList<>();
        
        try {
            // Use LLM to extract topics
            if (ollamaEnabled) {
                topics = extractTopicsWithLLM(transcript, video.getDurationSeconds());
            } else {
                topics = extractTopicsWithRegex(transcript);
            }
            
            // Save topics to database
            int order = 0;
            for (StructuredSummaryResponse.TopicData topicData : topics) {
                Topic topic = Topic.builder()
                        .topicName(topicData.getTopic())
                        .description(topicData.getDescription())
                        .startSeconds(topicData.getStartSeconds())
                        .endSeconds(topicData.getEndSeconds())
                        .startTime(topicData.getStartTime())
                        .endTime(topicData.getEndTime())
                        .sequenceOrder(order++)
                        .video(video)
                        .build();
                topicRepository.save(topic);
            }
            
            log.info("Successfully extracted {} topics from video: {}", topics.size(), request.getVideoId());
            return topics;
            
        } catch (Exception e) {
            log.error("Error extracting topics: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract topics: " + e.getMessage());
        }
    }
    
    /**
     * Extract topics using LLM (Ollama)
     */
    private List<StructuredSummaryResponse.TopicData> extractTopicsWithLLM(
            String transcript, Long duration) {
        
        String prompt = buildTopicExtractionPrompt(transcript, duration);
        
        String response = ollamaClient.generateTextSync(prompt);
        
        return parseTopicsFromLLMResponse(response, duration);
    }
    
    /**
     * Extract topics using regex-based pattern matching (fallback)
     */
    private List<StructuredSummaryResponse.TopicData> extractTopicsWithRegex(String transcript) {
        List<StructuredSummaryResponse.TopicData> topics = new ArrayList<>();
        
        // Simple pattern for topic transitions
        Pattern pattern = Pattern.compile(
                "(?i)(topic|chapter|section|part)\\s*[:\\-]?\\s*([^\\n]+)",
                Pattern.MULTILINE
        );
        
        Matcher matcher = pattern.matcher(transcript);
        
        while (matcher.find()) {
            String topicName = matcher.group(2).trim();
            int startSeconds = estimateTimeFromPosition(matcher.start(), transcript);
            int endSeconds = startSeconds + 300; // Default 5 minutes
            
            StructuredSummaryResponse.TopicData topic = StructuredSummaryResponse.TopicData.builder()
                    .topic(topicName)
                    .startSeconds(startSeconds)
                    .endSeconds(endSeconds)
                    .startTime(formatSeconds(startSeconds))
                    .endTime(formatSeconds(endSeconds))
                    .build();
            
            topics.add(topic);
        }
        
        return topics;
    }
    
    /**
     * Parse topics from LLM response JSON
     */
    private List<StructuredSummaryResponse.TopicData> parseTopicsFromLLMResponse(
            String response, Long duration) {
        
        List<StructuredSummaryResponse.TopicData> topics = new ArrayList<>();
        
        try {
            // Extract JSON array from response
            int startIdx = response.indexOf("[");
            int endIdx = response.lastIndexOf("]");
            
            if (startIdx == -1 || endIdx == -1) {
                log.warn("Could not find JSON array in LLM response");
                return topics;
            }
            
            String jsonArray = response.substring(startIdx, endIdx + 1);
            
            // Simple JSON parsing (in production, use Jackson or Gson)
            String[] items = jsonArray.split("\\},\\s*\\{");
            
            for (String item : items) {
                StructuredSummaryResponse.TopicData topic = parseTopicItem(item, duration);
                if (topic != null) {
                    topics.add(topic);
                }
            }
            
            return topics;
            
        } catch (Exception e) {
            log.error("Error parsing LLM response: {}", e.getMessage());
            return topics;
        }
    }
    
    /**
     * Parse individual topic item from JSON
     */
    private StructuredSummaryResponse.TopicData parseTopicItem(String item, Long duration) {
        try {
            String topic = extractJsonValue(item, "topic");
            String description = extractJsonValue(item, "description");
            String startTime = extractJsonValue(item, "start");
            String endTime = extractJsonValue(item, "end");
            
            if (topic == null || topic.isBlank()) {
                return null;
            }
            
            Integer startSeconds = timeToSeconds(startTime);
            Integer endSeconds = timeToSeconds(endTime);
            
            return StructuredSummaryResponse.TopicData.builder()
                    .topic(topic)
                    .description(description)
                    .startSeconds(startSeconds)
                    .endSeconds(endSeconds)
                    .startTime(formatSeconds(startSeconds))
                    .endTime(formatSeconds(endSeconds))
                    .build();
                    
        } catch (Exception e) {
            log.debug("Error parsing topic item: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Build prompt for topic extraction
     */
    private String buildTopicExtractionPrompt(String transcript, Long duration) {
        return String.format("""
                Analyze the following transcript and identify all distinct topics, chapters, or discussion points.
                For each topic, provide:
                1. Topic name
                2. Brief description
                3. Approximate start time (HH:MM:SS format)
                4. Approximate end time (HH:MM:SS format)
                
                Return ONLY a valid JSON array with this structure:
                [
                  {
                    "topic": "Topic Name",
                    "description": "Brief description of the topic",
                    "start": "HH:MM:SS",
                    "end": "HH:MM:SS"
                  }
                ]
                
                Video duration: %d seconds
                
                Transcript:
                %s
                """, duration, transcript);
    }
    
    /**
     * Extract value from simple JSON string
     */
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * Convert HH:MM:SS format to seconds
     */
    private Integer timeToSeconds(String time) {
        if (time == null || time.isBlank()) {
            return 0;
        }
        
        try {
            String[] parts = time.split(":");
            int hours = parts.length > 2 ? Integer.parseInt(parts[0]) : 0;
            int minutes = parts.length > 1 ? Integer.parseInt(parts[parts.length - 2]) : 0;
            int seconds = Integer.parseInt(parts[parts.length - 1]);
            
            return hours * 3600 + minutes * 60 + seconds;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Convert seconds to HH:MM:SS format
     */
    private String formatSeconds(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "00:00:00";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * Estimate time position based on character position in transcript
     */
    private int estimateTimeFromPosition(int charPosition, String transcript) {
        // Rough estimate: assume 2 characters per second of audio
        return Math.max(0, charPosition / 2);
    }
    
    /**
     * Get topics for a video
     */
    @Transactional(readOnly = true)
    public List<StructuredSummaryResponse.TopicData> getTopicsForVideo(Long videoId) {
        log.debug("Fetching topics for video: {}", videoId);
        
        List<Topic> topics = topicRepository.findByVideoIdOrderBySequenceOrderAsc(videoId);
        
        return topics.stream()
                .map(topic -> StructuredSummaryResponse.TopicData.builder()
                        .topic(topic.getTopicName())
                        .description(topic.getDescription())
                        .startSeconds(topic.getStartSeconds())
                        .endSeconds(topic.getEndSeconds())
                        .startTime(topic.getStartTime())
                        .endTime(topic.getEndTime())
                        .build())
                .toList();
    }
}
