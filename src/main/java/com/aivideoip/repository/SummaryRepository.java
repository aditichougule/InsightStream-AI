package com.aivideoip.repository;

import com.aivideoip.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Summary entity
 */
@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {
    
    Optional<Summary> findByVideoId(Long videoId);
    
    boolean existsByVideoId(Long videoId);
}
