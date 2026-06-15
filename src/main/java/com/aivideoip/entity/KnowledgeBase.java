package com.aivideoip.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity for managing knowledge bases (collections of videos/courses)
 */
@Entity
@Table(name = "knowledge_bases", indexes = {
    @Index(name = "idx_kb_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBase extends BaseEntity {
    
    @Column(nullable = false, unique = true, length = 255)
    private String name;
    
    @Column(length = 2000)
    private String description;
    
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "kb_videos",
        joinColumns = @JoinColumn(name = "kb_id"),
        inverseJoinColumns = @JoinColumn(name = "video_id")
    )
    private List<Video> videos = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean indexed = false;
    
    @Column(length = 50)
    private String status = "ACTIVE";  // ACTIVE, INDEXING, FAILED
    
    @Column
    private Integer progress = 0;  // 0-100 during indexing
    
    @Column
    private Long lastIndexedAt;
    
    @Column(length = 500)
    private String chromaCollectionName;  // Name of ChromaDB collection for this KB
}
