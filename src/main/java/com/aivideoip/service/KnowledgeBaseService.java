package com.aivideoip.service;

import com.aivideoip.dto.KBIndexRequest;
import com.aivideoip.dto.KBStatusDTO;
import com.aivideoip.entity.KnowledgeBase;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.KnowledgeBaseRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing multi-video knowledge bases
 * 
 * Supports:
 * - Creating/managing knowledge bases (courses, document collections)
 * - Indexing multiple videos into unified vector stores
 * - Cross-video semantic search
 * - Status tracking and progress monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KnowledgeBaseService {
    
    private final KnowledgeBaseRepository kbRepository;
    private final VideoRepository videoRepository;
    private final EmbeddingPipelineService embeddingPipelineService;
    
    /**
     * Create and index a new knowledge base from multiple videos
     * 
     * @param request KB creation request with video IDs
     * @return Status of the created knowledge base
     */
    public KBStatusDTO createAndIndexKnowledgeBase(KBIndexRequest request) {
        log.info("Creating knowledge base: {} with {} videos", 
                request.getKbName(), request.getVideoIds().size());
        
        // Step 1: Check if KB already exists
        if (kbRepository.findByName(request.getKbName()).isPresent() && !request.getReindex()) {
            throw new IllegalStateException("Knowledge base '" + request.getKbName() + "' already exists");
        }
        
        // Step 2: Validate all videos exist
        List<Video> videos = validateVideos(request.getVideoIds());
        
        // Step 3: Create or get KB
        KnowledgeBase kb = kbRepository.findByName(request.getKbName())
                .orElse(KnowledgeBase.builder()
                        .name(request.getKbName())
                        .description(request.getDescription())
                        .chromaCollectionName(generateCollectionName(request.getKbName()))
                        .build());
        
        if (request.getReindex()) {
            kb.getVideos().clear();
            kb.setIndexed(false);
            log.info("Reindexing knowledge base: {}", request.getKbName());
        }
        
        // Step 4: Add videos to KB
        kb.getVideos().addAll(videos);
        kb.setStatus("INDEXING");
        kb.setProgress(0);
        
        KnowledgeBase savedKb = kbRepository.save(kb);
        
        // Step 5: Index embeddings for all videos (async in production)
        indexVideosEmbeddings(savedKb, videos);
        
        // Step 6: Mark as indexed
        savedKb.setIndexed(true);
        savedKb.setStatus("ACTIVE");
        savedKb.setProgress(100);
        savedKb.setLastIndexedAt(System.currentTimeMillis());
        kbRepository.save(savedKb);
        
        log.info("Successfully created and indexed knowledge base: {}", request.getKbName());
        
        return toStatusDTO(savedKb);
    }
    
    /**
     * Get knowledge base status and information
     */
    @Transactional(readOnly = true)
    public KBStatusDTO getKnowledgeBaseStatus(String kbName) {
        log.debug("Fetching knowledge base status: {}", kbName);
        
        KnowledgeBase kb = kbRepository.findByName(kbName)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + kbName));
        
        return toStatusDTO(kb);
    }
    
    /**
     * Add videos to existing knowledge base
     */
    public KBStatusDTO addVideosToKnowledgeBase(String kbName, Set<Long> videoIds) {
        log.info("Adding {} videos to knowledge base: {}", videoIds.size(), kbName);
        
        KnowledgeBase kb = kbRepository.findByName(kbName)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + kbName));
        
        List<Video> newVideos = validateVideos(videoIds);
        
        // Add only new videos
        for (Video video : newVideos) {
            if (!kb.getVideos().contains(video)) {
                kb.getVideos().add(video);
            }
        }
        
        kb.setIndexed(false);
        kb.setStatus("INDEXING");
        
        KnowledgeBase updatedKb = kbRepository.save(kb);
        
        // Index new videos
        indexVideosEmbeddings(updatedKb, newVideos);
        
        updatedKb.setIndexed(true);
        updatedKb.setStatus("ACTIVE");
        updatedKb.setLastIndexedAt(System.currentTimeMillis());
        kbRepository.save(updatedKb);
        
        return toStatusDTO(updatedKb);
    }
    
    /**
     * Remove video from knowledge base
     */
    public KBStatusDTO removeVideoFromKnowledgeBase(String kbName, Long videoId) {
        log.info("Removing video {} from knowledge base: {}", videoId, kbName);
        
        KnowledgeBase kb = kbRepository.findByName(kbName)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + kbName));
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
        
        kb.getVideos().remove(video);
        kbRepository.save(kb);
        
        return toStatusDTO(kb);
    }
    
    /**
     * Delete knowledge base
     */
    public void deleteKnowledgeBase(String kbName) {
        log.info("Deleting knowledge base: {}", kbName);
        
        KnowledgeBase kb = kbRepository.findByName(kbName)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + kbName));
        
        kbRepository.delete(kb);
    }
    
    /**
     * List all knowledge bases
     */
    @Transactional(readOnly = true)
    public List<KBStatusDTO> listAllKnowledgeBases() {
        return kbRepository.findAll().stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get indexed knowledge bases only
     */
    @Transactional(readOnly = true)
    public List<KBStatusDTO> getIndexedKnowledgeBases() {
        return kbRepository.findByIndexedTrue().stream()
                .map(this::toStatusDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Validate that all videos exist
     */
    private List<Video> validateVideos(Set<Long> videoIds) {
        List<Video> videos = videoRepository.findAllById(videoIds);
        
        if (videos.size() != videoIds.size()) {
            throw new ResourceNotFoundException("One or more videos not found");
        }
        
        return videos;
    }
    
    /**
     * Index embeddings for all videos in a knowledge base
     */
    private void indexVideosEmbeddings(KnowledgeBase kb, List<Video> videos) {
        for (int i = 0; i < videos.size(); i++) {
            Video video = videos.get(i);
            try {
                log.debug("Indexing embeddings for video: {} in KB: {}", video.getId(), kb.getName());
                embeddingPipelineService.processVideoEmbeddings(video.getId());
                
                // Update progress
                kb.setProgress((i + 1) * 100 / videos.size());
                kbRepository.save(kb);
            } catch (Exception e) {
                log.error("Failed to index video {} in KB {}: {}", 
                        video.getId(), kb.getName(), e.getMessage());
                kb.setStatus("FAILED");
                kbRepository.save(kb);
                throw new RuntimeException("Failed to index knowledge base: " + e.getMessage());
            }
        }
    }
    
    /**
     * Generate ChromaDB collection name from KB name
     */
    private String generateCollectionName(String kbName) {
        return "kb_" + kbName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }
    
    /**
     * Convert KnowledgeBase entity to StatusDTO
     */
    private KBStatusDTO toStatusDTO(KnowledgeBase kb) {
        List<KBStatusDTO.KBVideoInfo> videoInfos = kb.getVideos().stream()
                .map(v -> KBStatusDTO.KBVideoInfo.builder()
                        .videoId(v.getId())
                        .title(v.getTitle())
                        .chunkCount((int) v.getTranscriptChunks().size())
                        .embedded(true)  // Simplified: assume embedded if in KB
                        .build())
                .collect(Collectors.toList());
        
        return KBStatusDTO.builder()
                .kbName(kb.getName())
                .description(kb.getDescription())
                .videoCount(kb.getVideos().size())
                .chunkCount(kb.getVideos().stream()
                        .mapToLong(v -> v.getTranscriptChunks().size())
                        .sum())
                .indexed(kb.getIndexed())
                .createdAt(kb.getCreatedAt() != null ? kb.getCreatedAt().getTime() : null)
                .lastUpdatedAt(kb.getLastIndexedAt())
                .videos(videoInfos)
                .status(kb.getStatus())
                .progress(kb.getProgress())
                .build();
    }
}
