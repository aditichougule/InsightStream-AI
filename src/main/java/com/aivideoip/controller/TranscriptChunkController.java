package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.service.TranscriptChunkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/videos/{videoId}/transcript-chunks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transcript Chunks", description = "Manage transcript chunks")
public class TranscriptChunkController {

    private final TranscriptChunkService service;

    @PostMapping
    @Operation(summary = "Create a new transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> create(
            @PathVariable Long videoId,
            @Valid @RequestBody TranscriptChunkDTO dto) {
        log.info("Creating transcript chunk for video: {}", videoId);
        var chunk = service.createChunk(videoId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(chunk, "Transcript chunk created"));
    }

    @GetMapping
    @Operation(summary = "Get all transcript chunks for a video")
    public ResponseEntity<ApiResponse<Page<TranscriptChunkDTO>>> list(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Listing chunks for video: {}", videoId);
        var pageable = PageRequest.of(page, size);
        var chunks = service.getVideoChunks(videoId, pageable);
        return ResponseEntity.ok(ApiResponse.success(chunks));
    }

    @GetMapping("/{chunkId}")
    @Operation(summary = "Get a specific transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> get(@PathVariable Long chunkId) {
        log.debug("Fetching chunk: {}", chunkId);
        var chunk = service.getChunkById(chunkId);
        return ResponseEntity.ok(ApiResponse.success(chunk));
    }

    @PutMapping("/{chunkId}")
    @Operation(summary = "Update a transcript chunk")
    public ResponseEntity<ApiResponse<TranscriptChunkDTO>> update(
            @PathVariable Long chunkId,
            @Valid @RequestBody TranscriptChunkDTO dto) {
        log.info("Updating chunk: {}", chunkId);
        var updated = service.updateChunk(chunkId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Updated"));
    }

    @DeleteMapping("/{chunkId}")
    @Operation(summary = "Delete a transcript chunk")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long chunkId) {
        log.info("Deleting chunk: {}", chunkId);
        service.deleteChunk(chunkId);
        return ResponseEntity.ok(ApiResponse.success("Chunk deleted"));
    }

    @PostMapping("/chunking/apply")
    @Operation(summary = "Apply intelligent semantic chunking to a transcript")
    public ResponseEntity<ApiResponse<List<TranscriptChunkDTO>>> applySemanticChunking(
            @PathVariable Long videoId,
            @RequestParam String transcript,
            @RequestParam(defaultValue = "0") Integer startTime,
            @RequestParam Integer endTime) {
        log.info("Applying semantic chunking to video: {}", videoId);
        
        List<TranscriptChunkDTO> chunks = service.performSemanticChunking(videoId, transcript, startTime, endTime);
        
        return ResponseEntity.ok(ApiResponse.success(chunks, 
                "Semantic chunking completed: " + chunks.size() + " chunks created"));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get chunking statistics for a video")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChunkingStatistics(
            @PathVariable Long videoId) {
        log.debug("Fetching chunking statistics for video: {}", videoId);
        
        Map<String, Object> stats = service.getChunkingStatistics(videoId);
        
        return ResponseEntity.ok(ApiResponse.success(stats, "Statistics retrieved successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search chunks by keyword")
    public ResponseEntity<ApiResponse<List<TranscriptChunkDTO>>> searchChunks(
            @PathVariable Long videoId,
            @RequestParam String keyword) {
        log.debug("Searching chunks in video {} for keyword: {}", videoId, keyword);
        
        List<TranscriptChunkDTO> results = service.searchChunks(videoId, keyword);
        
        return ResponseEntity.ok(ApiResponse.success(results, 
                "Search completed: " + results.size() + " results found"));
    }
}
