package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.SemanticSearchRequest;
import com.aivideoip.dto.SemanticSearchResult;
import com.aivideoip.service.SemanticSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for semantic search API
 * 
 * Endpoints:
 * - POST /api/search - Semantic search across transcripts
 * - GET /api/search/health - Health check
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SemanticSearchController {
    
    private final SemanticSearchService semanticSearchService;
    
    /**
     * Perform semantic search on video transcripts
     * 
     * @param request Search request with query and optional filters
     * @return Search results with ranked matches and timestamps
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SemanticSearchResult>> search(
            @Valid @RequestBody SemanticSearchRequest request) {
        
        log.info("Received semantic search request: query='{}', videoId={}", 
                request.getQuery(), request.getVideoId());
        
        try {
            SemanticSearchResult result = semanticSearchService.search(request);
            
            return ResponseEntity.ok(ApiResponse.<SemanticSearchResult>builder()
                    .success(true)
                    .message("Search completed successfully. Found " + result.getTotalMatches() + " matches.")
                    .data(result)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid search request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<SemanticSearchResult>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error performing semantic search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<SemanticSearchResult>builder()
                    .success(false)
                    .message("Search failed: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Semantic search service is healthy")
                .data("OK")
                .statusCode(HttpStatus.OK.value())
                .build());
    }
}
