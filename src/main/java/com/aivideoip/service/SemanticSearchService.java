package com.aivideoip.service;

import com.aivideoip.dto.SemanticSearchRequest;
import com.aivideoip.dto.SemanticSearchResult;
import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.exception.ResourceNotFoundException;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for semantic search across video transcripts
 * 
 * Supports:
 * - Per-video search (filtered by videoId)
 * - Global search (across all videos)
 * - Configurable top-K and similarity threshold
 * - Context extraction with surrounding chunks
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SemanticSearchService {
    
    private final OllamaEmbeddingClient embeddingClient;
    private final TranscriptChunkRepository chunkRepository;
    private final VideoRepository videoRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;
    
    @Value("${app.semantic-search.top-k:10}")
    private Integer defaultTopK;
    
    @Value("${app.semantic-search.similarity-threshold:0.3}")
    private Float defaultThreshold;
    
    /**
     * Perform semantic search on transcripts
     * 
     * @param request Search request with query and filters
     * @return Search results with ranked matches
     */
    public SemanticSearchResult search(SemanticSearchRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Performing semantic search: query='{}', videoId={}, topK={}", 
                request.getQuery(), request.getVideoId(), request.getTopK());
        
        try {
            // Step 1: Validate video if specified
            if (request.getVideoId() != null) {
                videoRepository.findById(request.getVideoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
            }
            
            // Step 2: Generate embedding for query
            List<Double> queryEmbedding = embeddingClient.generateEmbedding(request.getQuery())
                    .block();
            
            if (queryEmbedding == null || queryEmbedding.isEmpty()) {
                log.warn("Failed to generate embedding for query");
                return buildEmptyResult(request, startTime);
            }
            
            log.debug("Generated query embedding with {} dimensions", queryEmbedding.size());
            
            // Step 3: Retrieve chunks based on video filter
            List<TranscriptChunk> chunks;
            if (request.getVideoId() != null) {
                chunks = chunkRepository.findByVideoIdOrderByStartTime(request.getVideoId());
            } else {
                chunks = chunkRepository.findAll();
            }
            
            if (chunks.isEmpty()) {
                log.warn("No transcript chunks found");
                return buildEmptyResult(request, startTime);
            }
            
            // Step 4: Calculate similarities and rank
            int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
            Float threshold = request.getSimilarityThreshold() != null ? 
                    request.getSimilarityThreshold() : defaultThreshold;
            
            List<ChunkScorePair> scoredChunks = chunks.stream()
                    .map(chunk -> {
                        List<Double> chunkEmbedding = deserializeEmbedding(chunk.getEmbedding());
                        float similarity = calculateCosineSimilarity(queryEmbedding, chunkEmbedding);
                        return new ChunkScorePair(chunk, similarity);
                    })
                    .filter(csp -> csp.score >= threshold)
                    .sorted(Comparator.comparing(ChunkScorePair::getScore).reversed())
                    .limit(topK)
                    .collect(Collectors.toList());
            
            log.info("Found {} relevant chunks above threshold {}", scoredChunks.size(), threshold);
            
            if (scoredChunks.isEmpty()) {
                return buildEmptyResult(request, startTime);
            }
            
            // Step 5: Build search results with context
            List<SemanticSearchResult.SearchMatch> matches = buildMatches(scoredChunks, chunks);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return SemanticSearchResult.builder()
                    .query(request.getQuery())
                    .videoId(request.getVideoId())
                    .matches(matches)
                    .totalMatches(matches.size())
                    .processingTimeMs(processingTime)
                    .build();
                    
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error performing semantic search: {}", e.getMessage(), e);
            throw new RuntimeException("Semantic search failed: " + e.getMessage());
        }
    }
    
    /**
     * Build search matches with context from surrounding chunks
     */
    private List<SemanticSearchResult.SearchMatch> buildMatches(
            List<ChunkScorePair> scoredChunks,
            List<TranscriptChunk> allChunks) {
        
        List<SemanticSearchResult.SearchMatch> matches = new ArrayList<>();
        Map<Long, TranscriptChunk> chunkMap = allChunks.stream()
                .collect(Collectors.toMap(TranscriptChunk::getId, c -> c));
        
        for (int rank = 0; rank < scoredChunks.size(); rank++) {
            ChunkScorePair csp = scoredChunks.get(rank);
            TranscriptChunk chunk = csp.chunk;
            
            // Extract context from neighboring chunks
            String context = extractContext(chunk, allChunks);
            
            SemanticSearchResult.SearchMatch match = SemanticSearchResult.SearchMatch.builder()
                    .chunkId(chunk.getId())
                    .videoTitle(chunk.getVideo().getTitle())
                    .text(chunk.getChunkText())
                    .startSeconds(chunk.getStartTime())
                    .endSeconds(chunk.getEndTime())
                    .startTime(formatSeconds(chunk.getStartTime()))
                    .endTime(formatSeconds(chunk.getEndTime()))
                    .similarityScore(csp.score)
                    .context(context)
                    .rank(rank + 1)
                    .build();
            
            matches.add(match);
        }
        
        return matches;
    }
    
    /**
     * Extract context from surrounding chunks
     */
    private String extractContext(TranscriptChunk targetChunk, List<TranscriptChunk> allChunks) {
        StringBuilder context = new StringBuilder();
        
        // Find adjacent chunks for context
        List<TranscriptChunk> sameVideoChunks = allChunks.stream()
                .filter(c -> c.getVideo().getId().equals(targetChunk.getVideo().getId()))
                .sorted(Comparator.comparing(TranscriptChunk::getStartTime))
                .collect(Collectors.toList());
        
        int currentIndex = sameVideoChunks.indexOf(targetChunk);
        
        // Include previous chunk for context
        if (currentIndex > 0) {
            TranscriptChunk previous = sameVideoChunks.get(currentIndex - 1);
            context.append("Previous: ").append(previous.getChunkText()).append(" ");
        }
        
        // Current chunk
        context.append("Current: ").append(targetChunk.getChunkText()).append(" ");
        
        // Include next chunk for context
        if (currentIndex < sameVideoChunks.size() - 1) {
            TranscriptChunk next = sameVideoChunks.get(currentIndex + 1);
            context.append("Next: ").append(next.getChunkText());
        }
        
        return context.toString().trim();
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private float calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.isEmpty() || vector2.isEmpty()) {
            return 0f;
        }
        
        if (vector1.size() != vector2.size()) {
            log.warn("Vector dimension mismatch: {} vs {}", vector1.size(), vector2.size());
            return 0f;
        }
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < vector1.size(); i++) {
            double a = vector1.get(i);
            double b = vector2.get(i);
            
            dotProduct += a * b;
            norm1 += a * a;
            norm2 += b * b;
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0f;
        }
        
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2)));
    }
    
    /**
     * Deserialize embedding from JSON string
     */
    private List<Double> deserializeEmbedding(String embeddingJson) {
        try {
            if (embeddingJson == null || embeddingJson.isBlank()) {
                return List.of();
            }
            @SuppressWarnings("unchecked")
            List<Double> embedding = objectMapper.readValue(embeddingJson, List.class);
            return embedding != null ? embedding : List.of();
        } catch (Exception e) {
            log.warn("Failed to deserialize embedding: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Format seconds to HH:MM:SS
     */
    private String formatSeconds(Integer seconds) {
        if (seconds == null || seconds < 0) {
            return "00:00:00";
        }
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * Build empty result when no matches found
     */
    private SemanticSearchResult buildEmptyResult(SemanticSearchRequest request, long startTime) {
        return SemanticSearchResult.builder()
                .query(request.getQuery())
                .videoId(request.getVideoId())
                .matches(List.of())
                .totalMatches(0)
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
    
    /**
     * Inner class for chunk-score pairing
     */
    private static class ChunkScorePair {
        TranscriptChunk chunk;
        float score;
        
        ChunkScorePair(TranscriptChunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
        
        public float getScore() {
            return score;
        }
    }
}
