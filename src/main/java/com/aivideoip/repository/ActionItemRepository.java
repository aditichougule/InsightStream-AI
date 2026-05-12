package com.aivideoip.repository;

import com.aivideoip.entity.ActionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ActionItem entity
 */
@Repository
public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    
    Page<ActionItem> findByVideoId(Long videoId, Pageable pageable);
    
    Page<ActionItem> findByVideoIdAndStatus(Long videoId, ActionItem.ActionStatus status, Pageable pageable);
    
    long countByVideoIdAndStatus(Long videoId, ActionItem.ActionStatus status);
}
