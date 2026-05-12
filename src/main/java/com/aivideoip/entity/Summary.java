package com.aivideoip.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Summary entity for storing video summaries
 */
@Entity
@Table(name = "summaries", indexes = {
    @Index(name = "idx_summary_video_id", columnList = "video_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Summary extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false, unique = true)
    private Video video;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String summaryText;

    @Column(columnDefinition = "TEXT")
    private String keyPoints;  // JSON array of key points

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SummaryType summaryType = SummaryType.GENERAL;

    public enum SummaryType {
        GENERAL,
        DETAILED,
        BRIEF
    }
}
