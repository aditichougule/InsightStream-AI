package com.aivideoip.controller;

import com.aivideoip.dto.ApiResponse;
import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.service.SummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/videos/{videoId}/summary")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Summaries", description = "Manage video summaries")
public class SummaryController {

    private final SummaryService service;

    @PostMapping
    @Operation(summary = "Create a summary for a video")
    public ResponseEntity<ApiResponse<SummaryDTO>> create(
            @PathVariable Long videoId,
            @Valid @RequestBody SummaryDTO dto) {
        log.info("Creating summary for video: {}", videoId);
        var summary = service.createSummary(videoId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(summary, "Summary created"));
    }

    @GetMapping
    @Operation(summary = "Get summary for a video")
    public ResponseEntity<ApiResponse<SummaryDTO>> get(@PathVariable Long videoId) {
        log.debug("Fetching summary for video: {}", videoId);
        var summary = service.getSummaryByVideoId(videoId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{summaryId}")
    @Operation(summary = "Get a specific summary")
    public ResponseEntity<ApiResponse<SummaryDTO>> getById(@PathVariable Long summaryId) {
        log.debug("Fetching summary: {}", summaryId);
        var summary = service.getSummaryById(summaryId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PutMapping("/{summaryId}")
    @Operation(summary = "Update a summary")
    public ResponseEntity<ApiResponse<SummaryDTO>> update(
            @PathVariable Long summaryId,
            @Valid @RequestBody SummaryDTO dto) {
        log.info("Updating summary: {}", summaryId);
        var updated = service.updateSummary(summaryId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Updated"));
    }

    @DeleteMapping("/{summaryId}")
    @Operation(summary = "Delete a summary")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long summaryId) {
        log.info("Deleting summary: {}", summaryId);
        service.deleteSummary(summaryId);
        return ResponseEntity.ok(ApiResponse.success("Summary deleted"));
    }
}
