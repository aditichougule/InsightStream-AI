package com.aivideoip.repository;

import com.aivideoip.entity.TranscriptChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for TranscriptChunk entity
 */
@Repository
public interface TranscriptChunkRepository extends JpaRepository<TranscriptChunk, Long> {
    
    Page<TranscriptChunk> findByVideoId(Long videoId, Pageable pageable);
    
    long countByVideoId(Long videoId);
}
