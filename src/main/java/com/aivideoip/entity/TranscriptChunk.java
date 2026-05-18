package com.aivideoip.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TranscriptChunk entity for storing video transcription chunks
 */
@Entity
@Table(name = "transcript_chunks", indexes = {
    @Index(name = "idx_video_id", columnList = "video_id"),
    @Index(name = "idx_start_time", columnList = "start_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(nullable = false)
    private Integer startTime;  // in seconds

    @Column(nullable = false)
    private Integer endTime;    // in seconds

    @Column(length = 255)
    private String speaker;

    @Column(length = 255)
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String embedding;  // For vector storage later
}
