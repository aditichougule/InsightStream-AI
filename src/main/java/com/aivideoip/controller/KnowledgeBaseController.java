package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.KBIndexRequest;
import com.aivideoip.dto.KBStatusDTO;
import com.aivideoip.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Knowledge Base API (Step 24)
 * 
 * Endpoints:
 * - POST /api/kb - Create and index new knowledge base
 * - GET /api/kb - List all knowledge bases
 * - GET /api/kb/{name} - Get specific KB status
 * - GET /api/kb/indexed/list - List indexed KBs
 * - POST /api/kb/{name}/add-videos - Add videos to KB
 * - DELETE /api/kb/{name}/videos/{videoId} - Remove video from KB
 * - DELETE /api/kb/{name} - Delete knowledge base
 */
@RestController
@RequestMapping("/api/kb")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseController {
    
    private final KnowledgeBaseService kbService;
    
    /**
     * Create and index a new knowledge base
     */
    @PostMapping
    public ResponseEntity<ApiResponse<KBStatusDTO>> createKnowledgeBase(
            @Valid @RequestBody KBIndexRequest request) {
        
        log.info("Creating knowledge base: {}", request.getKbName());
        
        try {
            KBStatusDTO status = kbService.createAndIndexKnowledgeBase(request);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<KBStatusDTO>builder()
                    .success(true)
                    .message("Knowledge base created and indexed successfully")
                    .data(status)
                    .statusCode(HttpStatus.CREATED.value())
                    .build());
                    
        } catch (IllegalStateException e) {
            log.warn("Knowledge base already exists: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<KBStatusDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error creating knowledge base: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<KBStatusDTO>builder()
                    .success(false)
                    .message("Failed to create knowledge base: " + e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
    
    /**
     * Get status of specific knowledge base
     */
    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<KBStatusDTO>> getKnowledgeBaseStatus(@PathVariable String name) {
        log.debug("Fetching KB status: {}", name);
        
        try {
            KBStatusDTO status = kbService.getKnowledgeBaseStatus(name);
            
            return ResponseEntity.ok(ApiResponse.<KBStatusDTO>builder()
                    .success(true)
                    .message("Knowledge base status retrieved")
                    .data(status)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error retrieving KB status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<KBStatusDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build());
        }
    }
    
    /**
     * List all knowledge bases
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KBStatusDTO>>> listKnowledgeBases() {
        log.debug("Listing all knowledge bases");
        
        List<KBStatusDTO> kbs = kbService.listAllKnowledgeBases();
        
        return ResponseEntity.ok(ApiResponse.<List<KBStatusDTO>>builder()
                .success(true)
                .message("Found " + kbs.size() + " knowledge bases")
                .data(kbs)
                .statusCode(HttpStatus.OK.value())
                .build());
    }
    
    /**
     * List indexed knowledge bases
     */
    @GetMapping("/indexed/list")
    public ResponseEntity<ApiResponse<List<KBStatusDTO>>> listIndexedKnowledgeBases() {
        log.debug("Listing indexed knowledge bases");
        
        List<KBStatusDTO> kbs = kbService.getIndexedKnowledgeBases();
        
        return ResponseEntity.ok(ApiResponse.<List<KBStatusDTO>>builder()
                .success(true)
                .message("Found " + kbs.size() + " indexed knowledge bases")
                .data(kbs)
                .statusCode(HttpStatus.OK.value())
                .build());
    }
    
    /**
     * Add videos to existing knowledge base
     */
    @PostMapping("/{name}/add-videos")
    public ResponseEntity<ApiResponse<KBStatusDTO>> addVideosToKB(
            @PathVariable String name,
            @RequestBody KBIndexRequest request) {
        
        log.info("Adding {} videos to KB: {}", request.getVideoIds().size(), name);
        
        try {
            KBStatusDTO status = kbService.addVideosToKnowledgeBase(name, request.getVideoIds());
            
            return ResponseEntity.ok(ApiResponse.<KBStatusDTO>builder()
                    .success(true)
                    .message("Videos added and indexed successfully")
                    .data(status)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error adding videos to KB: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<KBStatusDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
    
    /**
     * Remove video from knowledge base
     */
    @DeleteMapping("/{name}/videos/{videoId}")
    public ResponseEntity<ApiResponse<KBStatusDTO>> removeVideoFromKB(
            @PathVariable String name,
            @PathVariable Long videoId) {
        
        log.info("Removing video {} from KB: {}", videoId, name);
        
        try {
            KBStatusDTO status = kbService.removeVideoFromKnowledgeBase(name, videoId);
            
            return ResponseEntity.ok(ApiResponse.<KBStatusDTO>builder()
                    .success(true)
                    .message("Video removed from knowledge base")
                    .data(status)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error removing video from KB: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<KBStatusDTO>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build());
        }
    }
    
    /**
     * Delete knowledge base
     */
    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<String>> deleteKnowledgeBase(@PathVariable String name) {
        log.info("Deleting knowledge base: {}", name);
        
        try {
            kbService.deleteKnowledgeBase(name);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Knowledge base deleted successfully")
                    .data(name)
                    .statusCode(HttpStatus.OK.value())
                    .build());
                    
        } catch (Exception e) {
            log.error("Error deleting KB: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .build());
        }
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Knowledge base service is healthy")
                .data("OK")
                .statusCode(HttpStatus.OK.value())
                .build());
    }
}
