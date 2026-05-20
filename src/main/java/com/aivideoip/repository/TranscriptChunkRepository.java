package com.aivideoip.repository;

import com.aivideoip.entity.TranscriptChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TranscriptChunk entity
 */
@Repository
public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {
    
    Page<TranscriptChunk> findByVideoId(Long videoId, Pageable pageable);
    
    List<TranscriptChunk> findByVideoIdOrderByStartTime(Long videoId);
    
    long countByVideoId(Long videoId);
    
    void deleteByVideoId(Long videoId);
}
