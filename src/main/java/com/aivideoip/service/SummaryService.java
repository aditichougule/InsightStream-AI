package com.aivideoip.service;

import com.aivideoip.dto.SummaryDTO;
import com.aivideoip.entity.Summary;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.SummaryRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing video summaries
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SummaryService {

    private final SummaryRepository summaryRepository;
    private final VideoRepository videoRepository;

    public SummaryDTO createSummary(Long videoId, SummaryDTO dto) {
        log.info("Creating summary for video: {}", videoId);
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        if (summaryRepository.existsByVideoId(videoId)) {
            throw new IllegalStateException("Summary already exists for this video");
        }

        Summary summary = Summary.builder()
                .video(video)
                .summaryText(dto.getSummaryText())
                .keyPoints(dto.getKeyPoints())
                .summaryType(Summary.SummaryType.valueOf(dto.getSummaryType()))
                .build();

        Summary saved = summaryRepository.save(summary);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public SummaryDTO getSummaryByVideoId(Long videoId) {
        log.debug("Fetching summary for video: {}", videoId);
        
        return summaryRepository.findByVideoId(videoId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not found"));
    }

    @Transactional(readOnly = true)
    public SummaryDTO getSummaryById(Long summaryId) {
        log.debug("Fetching summary: {}", summaryId);
        
        return summaryRepository.findById(summaryId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not found"));
    }

    public SummaryDTO updateSummary(Long summaryId, SummaryDTO dto) {
        log.info("Updating summary: {}", summaryId);
        
        Summary summary = summaryRepository.findById(summaryId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not found"));

        summary.setSummaryText(dto.getSummaryText());
        summary.setKeyPoints(dto.getKeyPoints());
        summary.setSummaryType(Summary.SummaryType.valueOf(dto.getSummaryType()));

        Summary updated = summaryRepository.save(summary);
        return mapToDTO(updated);
    }

    public void deleteSummary(Long summaryId) {
        log.info("Deleting summary: {}", summaryId);
        
        if (!summaryRepository.existsById(summaryId)) {
            throw new ResourceNotFoundException("Summary not found");
        }
        
        summaryRepository.deleteById(summaryId);
    }

    private SummaryDTO mapToDTO(Summary summary) {
        return SummaryDTO.builder()
                .id(summary.getId())
                .videoId(summary.getVideo().getId())
                .summaryText(summary.getSummaryText())
                .keyPoints(summary.getKeyPoints())
                .summaryType(summary.getSummaryType().toString())
                .build();
    }
}
