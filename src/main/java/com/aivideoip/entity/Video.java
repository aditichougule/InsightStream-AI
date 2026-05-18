package com.aivideoip.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Video entity for storing video metadata
 */
@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoSource source;

    @Column(length = 500)
    private String thumbnailUrl;

    @Column
    private Long durationSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @Column(length = 2000)
    private String errorMessage;

    @Column(length = 255)
    private String videoId;

    @Column(length = 255)
    private String fileName;

    @Column(length = 500)
    private String filePath;

    @Column
    private Long fileSize;

    @Column(length = 500)
    private String audioFilePath;

    public enum VideoSource {
        YOUTUBE, UPLOADED, URL, PODCAST, WEBINAR
    }

    public enum ProcessingStatus {
        PENDING, DOWNLOADING, TRANSCRIBING, SUMMARIZING, COMPLETED, FAILED
    }
}
