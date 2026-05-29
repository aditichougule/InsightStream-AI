package com.aivideoip.repository;

import com.aivideoip.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Topic entity
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    List<Topic> findByVideoIdOrderBySequenceOrderAsc(Long videoId);
    
    Optional<Topic> findByVideoIdAndSequenceOrder(Long videoId, Integer sequenceOrder);
    
    @Query("SELECT t FROM Topic t WHERE t.video.id = :videoId AND t.startSeconds <= :seconds AND t.endSeconds >= :seconds")
    Optional<Topic> findTopicByTimestamp(@Param("videoId") Long videoId, @Param("seconds") Integer seconds);
    
    void deleteByVideoId(Long videoId);
}
