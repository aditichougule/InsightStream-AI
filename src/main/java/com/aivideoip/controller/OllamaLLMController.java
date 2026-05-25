package com.aivideoip.controller;

import com.aivideoip.dto.ActionItemDTO;
import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.service.OllamaLLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Ollama-based local LLM operations
 * All operations use local models (llama3, mistral, qwen2.5, etc.)
 * No cloud API calls or subscriptions required
 */
@RestController
@RequestMapping("/api/ollama/llm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ollama LLM Operations", description = "Local LLM operations using Ollama service")
public class OllamaLLMController {

    private final OllamaLLMService ollamaLLMService;

    /**
     * Generate comprehensive summary from video transcript
     * Supports: GENERAL, DETAILED, BRIEF
     *
     * @param videoId the video ID
     * @param summaryType type of summary (GENERAL, DETAILED, BRIEF)
     * @return SummaryDTO with generated summary
     */
    @PostMapping("/summarize/{videoId}")
    @Operation(summary = "Generate video summary", description = "Generate a summary of specified type using local LLM")
    public ResponseEntity<Map<String, Object>> generateSummary(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "GENERAL") String summaryType) {

        log.info("Generating {} summary for video: {}", summaryType, videoId);

        try {
            SummaryDTO summary = ollamaLLMService.generateSummary(videoId, summaryType);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Summary generated successfully using local Ollama");
            response.put("data", summary);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating summary", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Extract action items from video transcript
     *
     * @param videoId the video ID
     * @return list of extracted ActionItemDTOs
     */
    @PostMapping("/action-items/{videoId}")
    @Operation(summary = "Extract action items", description = "Extract tasks and deliverables from video transcript")
    public ResponseEntity<Map<String, Object>> extractActionItems(
            @PathVariable Long videoId) {

        log.info("Extracting action items for video: {}", videoId);

        try {
            List<ActionItemDTO> actionItems = ollamaLLMService.extractActionItems(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Action items extracted successfully using local Ollama");
            response.put("data", actionItems);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error extracting action items", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Generate chunk-level summaries for all transcript chunks
     *
     * @param videoId the video ID
     * @return list of chunk summaries with timestamps
     */
    @PostMapping("/chunk-summaries/{videoId}")
    @Operation(summary = "Generate chunk summaries", description = "Generate summaries for each transcript chunk")
    public ResponseEntity<Map<String, Object>> generateChunkSummaries(
            @PathVariable Long videoId) {

        log.info("Generating chunk summaries for video: {}", videoId);

        try {
            List<Map<String, Object>> chunkSummaries = ollamaLLMService.generateChunkSummaries(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Chunk summaries generated successfully using local Ollama");
            response.put("data", chunkSummaries);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating chunk summaries", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Extract named entities from video transcript
     * Extracts: people, organizations, locations, concepts, numbers
     *
     * @param videoId the video ID
     * @return map of extracted entities by type
     */
    @PostMapping("/entities/{videoId}")
    @Operation(summary = "Extract entities", description = "Extract named entities from transcript")
    public ResponseEntity<Map<String, Object>> extractEntities(
            @PathVariable Long videoId) {

        log.info("Extracting entities for video: {}", videoId);

        try {
            Map<String, List<String>> entities = ollamaLLMService.extractEntities(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Entities extracted successfully using local Ollama");
            response.put("data", entities);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error extracting entities", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Generate questions and answers from transcript
     *
     * @param videoId the video ID
     * @param numQuestions number of Q&A pairs to generate
     * @return list of Q&A pairs
     */
    @PostMapping("/qa/{videoId}")
    @Operation(summary = "Generate Q&A pairs", description = "Generate questions and answers from transcript")
    public ResponseEntity<Map<String, Object>> generateQuestionAnswers(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "5") int numQuestions) {

        log.info("Generating {} Q&A pairs for video: {}", numQuestions, videoId);

        try {
            List<Map<String, String>> qaList = ollamaLLMService.generateQuestionsAnswers(videoId, numQuestions);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Q&A pairs generated successfully using local Ollama");
            response.put("data", qaList);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating Q&A", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Generate highlights from video transcript
     *
     * @param videoId the video ID
     * @return list of key highlights with timestamps
     */
    @PostMapping("/highlights/{videoId}")
    @Operation(summary = "Generate highlights", description = "Identify key highlights from transcript")
    public ResponseEntity<Map<String, Object>> generateHighlights(
            @PathVariable Long videoId) {

        log.info("Generating highlights for video: {}", videoId);

        try {
            List<Map<String, Object>> highlights = ollamaLLMService.generateHighlights(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Highlights generated successfully using local Ollama");
            response.put("data", highlights);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating highlights", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Analyze sentiment of video transcript
     *
     * @param videoId the video ID
     * @return sentiment analysis results
     */
    @PostMapping("/sentiment/{videoId}")
    @Operation(summary = "Analyze sentiment", description = "Analyze emotional tone and sentiment of transcript")
    public ResponseEntity<Map<String, Object>> analyzeSentiment(
            @PathVariable Long videoId) {

        log.info("Analyzing sentiment for video: {}", videoId);

        try {
            Map<String, Object> sentiment = ollamaLLMService.analyzeSentiment(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sentiment analyzed successfully using local Ollama");
            response.put("data", sentiment);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error analyzing sentiment", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Perform full comprehensive analysis on video
     * Combines: summary, action items, entities, Q&A, highlights, sentiment
     *
     * @param videoId the video ID
     * @return comprehensive analysis object
     */
    @PostMapping("/full-analysis/{videoId}")
    @Operation(summary = "Full video analysis", description = "Generate complete analysis including summary, action items, entities, Q&A, highlights, and sentiment")
    public ResponseEntity<Map<String, Object>> performFullAnalysis(
            @PathVariable Long videoId) {

        log.info("Performing full analysis for video: {}", videoId);

        try {
            Map<String, Object> fullAnalysis = new HashMap<>();
            fullAnalysis.put("summary", ollamaLLMService.generateSummary(videoId, "GENERAL"));
            fullAnalysis.put("actionItems", ollamaLLMService.extractActionItems(videoId));
            fullAnalysis.put("entities", ollamaLLMService.extractEntities(videoId));
            fullAnalysis.put("questions", ollamaLLMService.generateQuestionsAnswers(videoId, 5));
            fullAnalysis.put("highlights", ollamaLLMService.generateHighlights(videoId));
            fullAnalysis.put("sentiment", ollamaLLMService.analyzeSentiment(videoId));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Full analysis completed successfully using local Ollama");
            response.put("data", fullAnalysis);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error performing full analysis", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Get list of available models in Ollama
     * Shows all installed models ready for use
     *
     * @return list of available models with metadata
     */
    @GetMapping("/models")
    @Operation(summary = "Get available models", description = "List all available Ollama models")
    public ResponseEntity<Map<String, Object>> getAvailableModels() {

        log.info("Fetching available Ollama models");

        try {
            List<Map<String, Object>> models = ollamaLLMService.getAvailableModels();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Available models retrieved successfully");
            response.put("data", models);
            response.put("statusCode", 200);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching models", e);
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Check health of Ollama service
     * Ensures Ollama is running and accessible
     *
     * @return health status
     */
    @GetMapping("/health")
    @Operation(summary = "Check service health", description = "Verify Ollama service is running and accessible")
    public ResponseEntity<Map<String, Object>> checkServiceHealth() {

        log.info("Checking Ollama service health");

        try {
            boolean isHealthy = ollamaLLMService.checkServiceHealth();

            Map<String, Object> response = new HashMap<>();
            response.put("success", isHealthy);
            response.put("message", isHealthy ? "Ollama service is running" : "Ollama service is not responding");
            response.put("data", new HashMap<String, Object>() {{
                put("status", isHealthy ? "HEALTHY" : "UNAVAILABLE");
            }});
            response.put("statusCode", isHealthy ? 200 : 503);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking service health", e);
            return ResponseEntity.status(503)
                    .body(createErrorResponse("Ollama service is unavailable: " + e.getMessage()));
        }
    }

    // ============= Helper Methods =============

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("statusCode", 500);
        return errorResponse;
    }
}
