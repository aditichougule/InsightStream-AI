package com.aivideoip.controller;

import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.service.SummaryGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 13 - Summary Generation Controller
 * 
 * Endpoints for enhanced summary generation using Ollama
 * Supports synchronous and asynchronous processing
 */
@RestController
@RequestMapping("/api/summary/generation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Summary Generation", description = "Enhanced summary generation with notes, concepts, action items")
public class SummaryGenerationController {

    private final SummaryGenerationService summaryGenerationService;

    /**
     * Generate comprehensive summary with all components
     * Includes: concise notes, key concepts, action items, timestamps
     *
     * @param videoId the video ID
     * @return comprehensive summary
     */
    @PostMapping("/comprehensive/{videoId}")
    @Operation(summary = "Generate comprehensive summary", description = "Generate complete summary with notes, concepts, action items, and timestamps")
    public ResponseEntity<Map<String, Object>> generateComprehensiveSummary(
            @PathVariable Long videoId) {

        log.info("Generating comprehensive summary for video: {}", videoId);

        try {
            SummaryDTO summary = summaryGenerationService.generateComprehensiveSummary(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comprehensive summary generated successfully");
            response.put("data", summary);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating comprehensive summary", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Generate summary asynchronously
     * Non-blocking request using WebClient
     *
     * @param videoId the video ID
     * @return Mono<ResponseEntity> for async processing
     */
    @PostMapping("/comprehensive/{videoId}/async")
    @Operation(summary = "Generate comprehensive summary (async)", description = "Non-blocking comprehensive summary generation")
    public Mono<ResponseEntity<Map<String, Object>>> generateComprehensiveSummaryAsync(
            @PathVariable Long videoId) {

        log.info("Generating comprehensive summary asynchronously for video: {}", videoId);

        return summaryGenerationService.generateComprehensiveSummaryAsync(videoId)
                .map(summary -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Comprehensive summary generated successfully");
                    response.put("data", summary);
                    response.put("statusCode", 200);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Error generating async summary", error);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(createErrorResponse("Error: " + error.getMessage())));
                });
    }

    /**
     * Generate summary by type: BRIEF, GENERAL, DETAILED, COMPREHENSIVE
     *
     * @param videoId the video ID
     * @param summaryType the type of summary
     * @return generated summary
     */
    @PostMapping("/{summaryType}/{videoId}")
    @Operation(summary = "Generate summary by type", description = "Generate BRIEF, GENERAL, DETAILED, or COMPREHENSIVE summary")
    public ResponseEntity<Map<String, Object>> generateSummaryByType(
            @PathVariable Long videoId,
            @PathVariable String summaryType) {

        log.info("Generating {} summary for video: {}", summaryType, videoId);

        try {
            // Validate summary type
            try {
                com.aivideoip.entity.Summary.SummaryType.valueOf(summaryType.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Invalid summary type. Must be: BRIEF, GENERAL, DETAILED, COMPREHENSIVE"));
            }

            SummaryDTO summary = summaryGenerationService.generateSummaryByType(videoId, summaryType.toUpperCase());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Summary (" + summaryType + ") generated successfully");
            response.put("data", summary);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating {} summary", summaryType, e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Extract action items with timestamps
     *
     * @param videoId the video ID
     * @return list of action items with timestamps
     */
    @PostMapping("/action-items/{videoId}")
    @Operation(summary = "Extract action items", description = "Extract tasks and action items with timestamps")
    public ResponseEntity<Map<String, Object>> extractActionItems(
            @PathVariable Long videoId) {

        log.info("Extracting action items for video: {}", videoId);

        try {
            List<Map<String, Object>> actionItems = summaryGenerationService.extractActionItemsWithTimestamps(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Action items extracted successfully");
            response.put("data", actionItems);
            response.put("count", actionItems.size());
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error extracting action items", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Extract and rank key concepts
     *
     * @param videoId the video ID
     * @return list of key concepts with importance scores
     */
    @PostMapping("/key-concepts/{videoId}")
    @Operation(summary = "Extract key concepts", description = "Extract and rank key concepts by importance")
    public ResponseEntity<Map<String, Object>> extractKeyConcepts(
            @PathVariable Long videoId) {

        log.info("Extracting key concepts for video: {}", videoId);

        try {
            List<Map<String, Object>> concepts = summaryGenerationService.extractKeyConcepts(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Key concepts extracted successfully");
            response.put("data", concepts);
            response.put("count", concepts.size());
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error extracting key concepts", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    // ============= Helper Methods =============

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("statusCode", 500);
        return errorResponse;
    }
}
