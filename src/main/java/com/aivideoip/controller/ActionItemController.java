package com.aivideoip.controller;

import com.aivideoip.dto.ActionItemDTO;
import com.aivideoip.dto.ApiResponse;
import com.aivideoip.service.ActionItemService;
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

@RestController
@RequestMapping("/videos/{videoId}/action-items")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Action Items", description = "Manage action items")
public class ActionItemController {

    private final ActionItemService service;

    @PostMapping
    @Operation(summary = "Create an action item")
    public ResponseEntity<ApiResponse<ActionItemDTO>> create(
            @PathVariable Long videoId,
            @Valid @RequestBody ActionItemDTO dto) {
        log.info("Creating action item for video: {}", videoId);
        var item = service.createActionItem(videoId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Action item created"));
    }

    @GetMapping
    @Operation(summary = "Get all action items for a video")
    public ResponseEntity<ApiResponse<Page<ActionItemDTO>>> list(
            @PathVariable Long videoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Listing action items for video: {}", videoId);
        var pageable = PageRequest.of(page, size);
        var items = service.getVideoActionItems(videoId, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get action items by status")
    public ResponseEntity<ApiResponse<Page<ActionItemDTO>>> listByStatus(
            @PathVariable Long videoId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("Listing action items by status: {}", status);
        var pageable = PageRequest.of(page, size);
        var items = service.getActionItemsByStatus(videoId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/{itemId}")
    @Operation(summary = "Get a specific action item")
    public ResponseEntity<ApiResponse<ActionItemDTO>> get(@PathVariable Long itemId) {
        log.debug("Fetching action item: {}", itemId);
        var item = service.getActionItemById(itemId);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update an action item")
    public ResponseEntity<ApiResponse<ActionItemDTO>> update(
            @PathVariable Long itemId,
            @Valid @RequestBody ActionItemDTO dto) {
        log.info("Updating action item: {}", itemId);
        var updated = service.updateActionItem(itemId, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Updated"));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete an action item")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Long itemId) {
        log.info("Deleting action item: {}", itemId);
        service.deleteActionItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Action item deleted"));
    }
}
