package com.aivideoip.service;

import com.aivideoip.dto.TranscriptChunkDTO;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
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
 * Service for managing transcript chunks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TranscriptChunkService {

    private final TranscriptChunkRepository chunkRepository;
    private final VideoRepository videoRepository;

    public TranscriptChunkDTO createChunk(Long videoId, TranscriptChunkDTO dto) {
        log.info("Creating transcript chunk for video: {}", videoId);
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));

        TranscriptChunk chunk = TranscriptChunk.builder()
                .video(video)
                .chunkText(dto.getChunkText())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .speaker(dto.getSpeaker())
                .topic(dto.getTopic())
                .embedding(dto.getEmbedding())
                .build();

        TranscriptChunk saved = chunkRepository.save(chunk);
        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<TranscriptChunkDTO> getVideoChunks(Long videoId, Pageable pageable) {
        log.debug("Fetching chunks for video: {}", videoId);
        
        if (!videoRepository.existsById(videoId)) {
            throw new ResourceNotFoundException("Video not found");
        }

        Page<TranscriptChunk> chunks = chunkRepository.findByVideoId(videoId, pageable);
        List<TranscriptChunkDTO> dtos = chunks.getContent().stream()
                .map(this::mapToDTO)
                .toList();

        return new PageImpl<>(dtos, pageable, chunks.getTotalElements());
    }

    @Transactional(readOnly = true)
    public TranscriptChunkDTO getChunkById(Long chunkId) {
        log.debug("Fetching chunk: {}", chunkId);
        
        return chunkRepository.findById(chunkId)
                .map(this::mapToDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript chunk not found"));
    }

    public TranscriptChunkDTO updateChunk(Long chunkId, TranscriptChunkDTO dto) {
        log.info("Updating chunk: {}", chunkId);
        
        TranscriptChunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcript chunk not found"));

        chunk.setChunkText(dto.getChunkText());
        chunk.setStartTime(dto.getStartTime());
        chunk.setEndTime(dto.getEndTime());
        chunk.setSpeaker(dto.getSpeaker());
        chunk.setTopic(dto.getTopic());
        chunk.setEmbedding(dto.getEmbedding());

        TranscriptChunk updated = chunkRepository.save(chunk);
        return mapToDTO(updated);
    }

    public void deleteChunk(Long chunkId) {
        log.info("Deleting chunk: {}", chunkId);
        
        if (!chunkRepository.existsById(chunkId)) {
            throw new ResourceNotFoundException("Transcript chunk not found");
        }
        
        chunkRepository.deleteById(chunkId);
    }

    private TranscriptChunkDTO mapToDTO(TranscriptChunk chunk) {
        return TranscriptChunkDTO.builder()
                .id(chunk.getId())
                .videoId(chunk.getVideo().getId())
                .chunkText(chunk.getChunkText())
                .startTime(chunk.getStartTime())
                .endTime(chunk.getEndTime())
                .speaker(chunk.getSpeaker())
                .topic(chunk.getTopic())
                .embedding(chunk.getEmbedding())
                .build();
    }
    
}
