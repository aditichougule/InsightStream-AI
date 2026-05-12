package com.aivideoip.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ActionItem entity for storing action items extracted from videos
 */
@Entity
@Table(name = "action_items", indexes = {
    @Index(name = "idx_action_video_id", columnList = "video_id"),
    @Index(name = "idx_action_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActionItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "assigned_to", length = 255)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status = ActionStatus.PENDING;

    @Column
    private Integer timeReference;  // Reference time in seconds from video

    @Column(length = 255)
    private String priority;

    public enum ActionStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
