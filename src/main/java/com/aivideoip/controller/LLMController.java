package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.dto.ActionItemDTO;
import com.aivideoip.service.LLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for LLM-based operations: summaries, action items, entity extraction, Q&A
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "LLM Operations", description = "AI-powered summarization, extraction, and analysis")
public class LLMController {

    private final LLMService llmService;

    @PostMapping("/summarize/{videoId}")
    @Operation(summary = "Generate video summary using LLM")
    public ResponseEntity<ApiResponse<SummaryDTO>> generateSummary(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "GENERAL") String summaryType) {
        log.info("Generating {} summary for video: {}", summaryType, videoId);

        SummaryDTO summary = llmService.generateSummary(videoId, summaryType);

        return ResponseEntity.ok(ApiResponse.<SummaryDTO>builder()
                .success(true)
                .message("Summary generated successfully")
                .data(summary)
                .statusCode(200)
                .build());
    }

    @PostMapping("/action-items/{videoId}")
    @Operation(summary = "Extract action items from video transcript")
    public ResponseEntity<ApiResponse<List<ActionItemDTO>>> extractActionItems(
            @PathVariable Long videoId) {
        log.info("Extracting action items from video: {}", videoId);

        List<ActionItemDTO> items = llmService.extractActionItems(videoId);

        return ResponseEntity.ok(ApiResponse.<List<ActionItemDTO>>builder()
                .success(true)
                .message("Extracted " + items.size() + " action items")
                .data(items)
                .statusCode(200)
                .build());
    }

    @GetMapping("/chunk-summaries/{videoId}")
    @Operation(summary = "Generate brief summaries for each transcript chunk")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> generateChunkSummaries(
            @PathVariable Long videoId) {
        log.info("Generating chunk summaries for video: {}", videoId);

        List<Map<String, Object>> summaries = llmService.generateChunkSummaries(videoId);

        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .message("Generated summaries for " + summaries.size() + " chunks")
                .data(summaries)
                .statusCode(200)
                .build());
    }

    @GetMapping("/entities/{videoId}")
    @Operation(summary = "Extract important entities (people, organizations, concepts, etc.)")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> extractEntities(
            @PathVariable Long videoId) {
        log.info("Extracting entities from video: {}", videoId);

        Map<String, List<String>> entities = llmService.extractEntities(videoId);

        return ResponseEntity.ok(ApiResponse.<Map<String, List<String>>>builder()
                .success(true)
                .message("Extracted entities from transcript")
                .data(entities)
                .statusCode(200)
                .build());
    }

    @GetMapping("/qa/{videoId}")
    @Operation(summary = "Generate question-answer pairs for learning")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> generateQuestionsAnswers(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "5") int numQuestions) {
        log.info("Generating {} Q&A pairs for video: {}", numQuestions, videoId);

        List<Map<String, String>> qaList = llmService.generateQuestionsAnswers(videoId, numQuestions);

        return ResponseEntity.ok(ApiResponse.<List<Map<String, String>>>builder()
                .success(true)
                .message("Generated " + qaList.size() + " Q&A pairs")
                .data(qaList)
                .statusCode(200)
                .build());
    }

    @GetMapping("/highlights/{videoId}")
    @Operation(summary = "Generate timestamps for important moments in the video")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> generateHighlights(
            @PathVariable Long videoId) {
        log.info("Generating highlights for video: {}", videoId);

        List<Map<String, Object>> highlights = llmService.generateHighlights(videoId);

        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .message("Generated " + highlights.size() + " highlights")
                .data(highlights)
                .statusCode(200)
                .build());
    }

    @GetMapping("/sentiment/{videoId}")
    @Operation(summary = "Analyze sentiment and emotional tone of the transcript")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSentiment(
            @PathVariable Long videoId) {
        log.info("Analyzing sentiment for video: {}", videoId);

        Map<String, Object> sentiment = llmService.analyzeSentiment(videoId);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Sentiment analysis completed")
                .data(sentiment)
                .statusCode(200)
                .build());
    }

    @PostMapping("/full-analysis/{videoId}")
    @Operation(summary = "Perform complete AI analysis: summary, action items, entities, Q&A, highlights, sentiment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> performFullAnalysis(
            @PathVariable Long videoId) {
        log.info("Performing full AI analysis for video: {}", videoId);

        Map<String, Object> analysis = new java.util.HashMap<>();

        try {
            analysis.put("summary", llmService.generateSummary(videoId, "GENERAL"));
            analysis.put("actionItems", llmService.extractActionItems(videoId));
            analysis.put("entities", llmService.extractEntities(videoId));
            analysis.put("qaList", llmService.generateQuestionsAnswers(videoId, 5));
            analysis.put("highlights", llmService.generateHighlights(videoId));
            analysis.put("sentiment", llmService.analyzeSentiment(videoId));

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Full analysis completed successfully")
                    .data(analysis)
                    .statusCode(200)
                    .build());
        } catch (Exception e) {
            log.error("Error during full analysis", e);
            return ResponseEntity.status(500).body(ApiResponse.<Map<String, Object>>builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .statusCode(500)
                    .build());
        }
    }
}
