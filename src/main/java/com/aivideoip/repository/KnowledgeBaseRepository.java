package com.aivideoip.repository;

import com.aivideoip.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for KnowledgeBase entity
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    
    Optional<KnowledgeBase> findByName(String name);
    
    List<KnowledgeBase> findByIndexedTrue();
    
    List<KnowledgeBase> findByStatus(String status);
    
    long countByIndexedTrue();
}
