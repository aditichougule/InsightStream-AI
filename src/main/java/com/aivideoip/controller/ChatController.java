package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.ChatRequest;
import com.aivideoip.dto.ChatResponse;
import com.aivideoip.service.RAGChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for RAG Chat API
 * 
 * Endpoints:
 * - POST /api/chat/query - Process chat query with RAG pipeline
 * - GET /api/chat/health - Health check
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    
    private final RAGChatService ragChatService;
    
    /**
     * Process chat query using RAG pipeline
     * 
     * @param request Chat request with videoId and question
     * @return Chat response with answer and sources
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ChatResponse>> processQuery(
            @Valid @RequestBody ChatRequest request) {
        
        log.info("Received chat query for video: {}", request.getVideoId());
        
        try {
            ChatResponse response = ragChatService.processQuery(request);
            
            return ResponseEntity.ok(ApiResponse.<ChatResponse>builder()
                    .success(true)
                    .message("Query processed successfully")
                    .data(response)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<ChatResponse>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing chat query: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ChatResponse>builder()
                    .success(false)
                    .message("Failed to process query: " + e.getMessage())
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
                .message("RAG Chat service is healthy")
                .data("OK")
                .statusCode(HttpStatus.OK.value())
                .build());
    }
}
