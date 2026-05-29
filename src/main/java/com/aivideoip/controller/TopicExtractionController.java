package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.StructuredSummaryResponse;
import com.aivideoip.dto.TopicExtractionRequest;
import com.aivideoip.service.TopicExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for topic extraction operations
 */
@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Topics", description = "Extract and manage video topics/chapters")
public class TopicExtractionController {
    
    private final TopicExtractionService topicExtractionService;
    
    /**
     * Extract topics from video transcript
     */
    @PostMapping("/extract")
    @Operation(summary = "Extract topics from video transcript")
    public ResponseEntity<ApiResponse<List<StructuredSummaryResponse.TopicData>>> extractTopics(
            @Valid @RequestBody TopicExtractionRequest request) {
        
        log.info("Topic extraction request for video: {}", request.getVideoId());
        
        List<StructuredSummaryResponse.TopicData> topics = topicExtractionService.extractTopics(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(topics, "Topics extracted successfully"));
    }
    
    /**
     * Get topics for a specific video
     */
    @GetMapping("/video/{videoId}")
    @Operation(summary = "Get all topics for a video")
    public ResponseEntity<ApiResponse<List<StructuredSummaryResponse.TopicData>>> getTopicsForVideo(
            @PathVariable Long videoId) {
        
        log.debug("Fetching topics for video: {}", videoId);
        
        List<StructuredSummaryResponse.TopicData> topics = topicExtractionService.getTopicsForVideo(videoId);
        
        return ResponseEntity.ok(
                ApiResponse.success(topics, "Topics retrieved successfully"));
    }
}
