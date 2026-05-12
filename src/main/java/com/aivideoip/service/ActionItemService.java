package com.aivideoip.service;

import com.aivideoip.dto.ActionItemDTO;
import com.aivideoip.entity.ActionItem;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.ActionItemRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing action items
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActionItemService {

    private final ActionItemRepository actionItemRepository;
    private final VideoRepository videoRepository;

    public ActionItemDTO createActionItem(Long videoId, ActionItemDTO dto) {
        log.info("Creating action item for video: {}", videoId);
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        ActionItem item = ActionItem.builder()
                .video(video)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .assignedTo(dto.getAssignedTo())
                .status(ActionItem.ActionStatus.valueOf(dto.getStatus()))
                .timeReference(dto.getTimeReference())
                .priority(dto.getPriority())
                .build();

        ActionItem saved = actionItemRepository.save(item);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<ActionItemDTO> getVideoActionItems(Long videoId, Pageable pageable) {
        log.debug("Fetching action items for video: {}", videoId);
        
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        Page<ActionItem> items = actionItemRepository.findByVideoId(videoId, pageable);
        List<ActionItemDTO> dtos = items.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, items.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Page<ActionItemDTO> getActionItemsByStatus(Long videoId, String status, Pageable pageable) {
        log.debug("Fetching action items by status for video: {} - {}", videoId, status);
        
        Page<ActionItem> items = actionItemRepository.findByVideoIdAndStatus(
                videoId, 
                ActionItem.ActionStatus.valueOf(status), 
                pageable
        );
        List<ActionItemDTO> dtos = items.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, items.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ActionItemDTO getActionItemById(Long itemId) {
        log.debug("Fetching action item: {}", itemId);
        
        return actionItemRepository.findById(itemId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Action item not found"));
    }

    public ActionItemDTO updateActionItem(Long itemId, ActionItemDTO dto) {
        log.info("Updating action item: {}", itemId);
        
        ActionItem item = actionItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Action item not found"));

        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setAssignedTo(dto.getAssignedTo());
        item.setStatus(ActionItem.ActionStatus.valueOf(dto.getStatus()));
        item.setTimeReference(dto.getTimeReference());
        item.setPriority(dto.getPriority());

        ActionItem updated = actionItemRepository.save(item);
        return mapToDTO(updated);
    }

    public void deleteActionItem(Long itemId) {
        log.info("Deleting action item: {}", itemId);
        
        if (!actionItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Action item not found");
        }
        
        actionItemRepository.deleteById(itemId);
    }

    private ActionItemDTO mapToDTO(ActionItem item) {
        return ActionItemDTO.builder()
                .id(item.getId())
                .videoId(item.getVideo().getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .assignedTo(item.getAssignedTo())
                .status(item.getStatus().toString())
                .timeReference(item.getTimeReference())
                .priority(item.getPriority())
                .build();
    }
}
