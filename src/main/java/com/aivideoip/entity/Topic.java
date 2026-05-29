package com.aivideoip.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a topic/chapter in a video
 */
@Entity
@Table(name = "topics", indexes = {
    @Index(name = "idx_topic_video_id", columnList = "video_id"),
    @Index(name = "idx_topic_start_time", columnList = "start_seconds")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "video")
@ToString(exclude = "video")
public class Topic extends BaseEntity {
    
    @Column(name = "topic_name", nullable = false)
    private String topicName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "start_seconds")
    private Integer startSeconds;
    
    @Column(name = "end_seconds")
    private Integer endSeconds;
    
    @Column(name = "start_time")
    private String startTime;
    
    @Column(name = "end_time")
    private String endTime;
    
    @Column(name = "sequence_order")
    private Integer sequenceOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    @JsonBackReference
    private Video video;
}
