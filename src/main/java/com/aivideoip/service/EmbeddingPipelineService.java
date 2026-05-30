package com.aivideoip.service;

import com.aivideoip.entity.TranscriptChunk;
import com.aivideoip.entity.Video;
import com.aivideoip.repository.TranscriptChunkRepository;
import com.aivideoip.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service orchestrating the embedding pipeline
 * 
 * Step 18 — Local Embeddings: Use ollama pull nomic-embed-text
 * Step 19 — Embedding Pipeline: transcript chunk → embedding model → vector embedding → ChromaDB
 * 
 * Flow:
 * 1. Fetch transcript chunks for a video
 * 2. Generate embeddings using OllamaEmbeddingClient (local model: nomic-embed-text)
 * 3. Store embeddings in ChromaDB for vector search
 * 4. Enable semantic search capabilities across video content
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmbeddingPipelineService {

    private final TranscriptChunkRepository transcriptChunkRepository;
    private final VideoRepository videoRepository;
    private final OllamaEmbeddingClient ollamaEmbeddingClient;
    private final ChromaDBClient chromaDBClient;

    /**
     * Complete embedding pipeline for a video
     * 
     * Orchestrates:
     * 1. Initialize ChromaDB collection
     * 2. Fetch all transcript chunks for video
     * 3. Generate embeddings for each chunk
     * 4. Store embeddings in ChromaDB
     * 5. Update TranscriptChunk entities with embedding vectors
     * 
     * @param videoId Video ID to process
     * @return Mono with completion status
     */
    public Mono<Map<String, Object>> processVideoEmbeddings(Long videoId) {
        log.info("Starting embedding pipeline for video: {}", videoId);

        Optional<Video> videoOpt = videoRepository.findById(videoId);
        if (videoOpt.isEmpty()) {
            return Mono.error(new RuntimeException("Video not found: " + videoId));
        }

        Video video = videoOpt.get();
        log.info("Processing embeddings for video: {} ({})", video.getId(), video.getTitle());
        
        return chromaDBClient.initializeCollection()
                .flatMap(initialized -> processChunksForVideo(video))
                .map(result -> enrichResultWithStats(result, videoId))
                .doOnSuccess(result -> log.info("Embedding pipeline completed for video: {}", videoId))
                .doOnError(error -> log.error("Embedding pipeline failed for video: {}, error: {}", 
                        videoId, error.getMessage(), error));
    }

    /**
     * Process all transcript chunks for a video
     * 
     * @param video Video entity
     * @return Mono with processing results
     */
    private Mono<Map<String, Object>> processChunksForVideo(Video video) {
        log.debug("Fetching transcript chunks for video: {}", video.getId());

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(video.getId());

        if (chunks == null || chunks.isEmpty()) {
            log.warn("No transcript chunks found for video: {}", video.getId());
            return Mono.just(Map.of(
                    "videoId", video.getId(),
                    "chunksProcessed", 0,
                    "embeddingsGenerated", 0,
                    "embeddingsStored", 0,
                    "startTime", LocalDateTime.now()
            ));
        }

        log.info("Processing {} transcript chunks", chunks.size());

        return Flux.fromIterable(chunks)
                .flatMap(chunk -> processChunk(chunk, video.getId()))
                .collectList()
                .map(results -> {
                    long successCount = results.stream().filter(r -> r.getOrDefault("success", false).equals(true)).count();
                    return Map.of(
                            "videoId", video.getId(),
                            "chunksProcessed", chunks.size(),
                            "embeddingsGenerated", successCount,
                            "embeddingsStored", successCount,
                            "processingTime", LocalDateTime.now()
                    );
                });
    }

    /**
     * Process a single transcript chunk through the embedding pipeline
     * 
     * @param chunk Transcript chunk
     * @param videoId Video ID
     * @return Mono with chunk processing result
     */
    private Mono<Map<String, Object>> processChunk(TranscriptChunk chunk, Long videoId) {
        log.debug("Processing chunk: {} - Text length: {}", chunk.getId(), chunk.getChunkText().length());

        // Step 1: Generate embedding using Ollama
        return ollamaEmbeddingClient.generateEmbedding(chunk.getChunkText())
                .flatMap(embedding -> {
                    log.debug("Generated embedding for chunk: {}, dimension: {}", chunk.getId(), embedding.size());

                    // Step 2: Store embedding in ChromaDB
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("start_time", chunk.getStartTime());
                    metadata.put("end_time", chunk.getEndTime());
                    metadata.put("speaker", chunk.getSpeaker() != null ? chunk.getSpeaker() : "Unknown");
                    metadata.put("topic", chunk.getTopic() != null ? chunk.getTopic() : "");

                    String chunkId = "chunk_" + chunk.getId() + "_video_" + videoId;

                    return chromaDBClient.addEmbedding(
                            chunkId,
                            videoId,
                            chunk.getChunkText(),
                            embedding,
                            metadata
                    )
                    .flatMap(stored -> {
                        if (stored) {
                            log.debug("Successfully stored embedding for chunk: {}", chunk.getId());

                            // Step 3: Update chunk entity with embedding vector
                            chunk.setEmbedding(ollamaEmbeddingClient.embeddingToString(embedding));
                            transcriptChunkRepository.save(chunk);
                            
                            log.debug("Updated chunk with embedding vector: {}", chunk.getId());

                            Map<String, Object> successResult = new HashMap<>();
                            successResult.put("chunkId", chunk.getId());
                            successResult.put("success", true);
                            successResult.put("embeddingSize", embedding.size());
                            return Mono.just(successResult);
                        } else {
                            log.warn("Failed to store embedding for chunk: {}", chunk.getId());
                            Map<String, Object> failResult = new HashMap<>();
                            failResult.put("chunkId", chunk.getId());
                            failResult.put("success", false);
                            return Mono.just(failResult);
                        }
                    });
                })
                .doOnError(error -> log.error("Error processing chunk: {}, error: {}", 
                        chunk.getId(), error.getMessage(), error))
                .onErrorReturn(Map.of(
                        "chunkId", chunk.getId(),
                        "success", false
                ));
    }

    /**
     * Semantic search for similar chunks across a video
     * 
     * @param videoId Video ID to search within
     * @param queryText Text to search for
     * @param topK Number of results to return
     * @return Mono with list of similar chunks
     */
    public Mono<List<Map<String, Object>>> semanticSearch(Long videoId, String queryText, int topK) {
        log.info("Performing semantic search in video: {}, query: {}", videoId, queryText);

        return chromaDBClient.querySimilarByText(queryText, ollamaEmbeddingClient, videoId, topK)
                .map(results -> enrichSearchResults(results))
                .doOnError(error -> log.error("Semantic search failed for video: {}, error: {}", 
                        videoId, error.getMessage(), error))
                .onErrorReturn(List.of());
    }

    /**
     * Semantic search across all videos
     * 
     * @param queryText Text to search for
     * @param topK Number of results to return
     * @return Mono with list of similar chunks from any video
     */
    public Mono<List<Map<String, Object>>> globalSemanticSearch(String queryText, int topK) {
        log.info("Performing global semantic search for query: {}", queryText);

        return chromaDBClient.querySimilarByText(queryText, ollamaEmbeddingClient, null, topK)
                .map(results -> enrichSearchResults(results))
                .doOnError(error -> log.error("Global semantic search failed, error: {}", error.getMessage(), error))
                .onErrorReturn(List.of());
    }

    /**
     * Reprocess embeddings for a video (useful for updates)
     * 
     * @param videoId Video ID
     * @return Mono with reprocessing status
     */
    public Mono<Map<String, Object>> reprocessVideoEmbeddings(Long videoId) {
        log.info("Reprocessing embeddings for video: {}", videoId);

        return chromaDBClient.deleteVideoEmbeddings(videoId)
                .flatMap(deleted -> {
                    if (deleted) {
                        log.info("Deleted old embeddings for video: {}", videoId);
                        return processVideoEmbeddings(videoId);
                    } else {
                        log.warn("Failed to delete old embeddings for video: {}", videoId);
                        return Mono.error(new RuntimeException("Failed to delete old embeddings"));
                    }
                });
    }

    /**
     * Get embedding statistics for a video
     * 
     * @param videoId Video ID
     * @return Embedding statistics
     */
    @Transactional(readOnly = true)
    public Mono<Map<String, Object>> getEmbeddingStats(Long videoId) {
        log.debug("Fetching embedding stats for video: {}", videoId);

        List<TranscriptChunk> chunks = transcriptChunkRepository.findByVideoIdOrderByStartTime(videoId);
        
        long chunksWithEmbeddings = chunks.stream()
                .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isEmpty())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("videoId", videoId);
        stats.put("totalChunks", chunks.size());
        stats.put("chunksWithEmbeddings", chunksWithEmbeddings);
        stats.put("completionPercentage", chunks.isEmpty() ? 0 : (chunksWithEmbeddings * 100 / chunks.size()));
        stats.put("timestamp", LocalDateTime.now());

        return chromaDBClient.getCollectionStats()
                .map(collectionStats -> {
                    stats.put("collectionStats", collectionStats);
                    return stats;
                })
                .onErrorReturn(stats);
    }

    /**
     * Check if Ollama embedding model is available
     * 
     * @return true if model is ready
     */
    public Mono<Boolean> isEmbeddingModelReady() {
        log.debug("Checking if embedding model is ready");
        return ollamaEmbeddingClient.isModelAvailable();
    }

    /**
     * Check if ChromaDB is available
     * 
     * @return true if ChromaDB is healthy
     */
    public Mono<Boolean> isChromaDBReady() {
        log.debug("Checking if ChromaDB is healthy");
        return chromaDBClient.healthCheck();
    }

    /**
     * Initialize embedding infrastructure
     * Ensures both Ollama and ChromaDB are ready
     * 
     * @return true if both systems are ready
     */
    public Mono<Boolean> initializeEmbeddingInfrastructure() {
        log.info("Initializing embedding infrastructure");

        return Mono.zip(
                isEmbeddingModelReady(),
                isChromaDBReady(),
                chromaDBClient.initializeCollection()
        )
        .map(tuple -> {
            boolean ollamaReady = tuple.getT1();
            boolean chromadbReady = tuple.getT2();
            boolean collectionInitialized = tuple.getT3();

            log.info("Embedding infrastructure status - Ollama: {}, ChromaDB: {}, Collection: {}",
                    ollamaReady, chromadbReady, collectionInitialized);

            return ollamaReady && chromadbReady && collectionInitialized;
        })
        .doOnError(error -> log.error("Failed to initialize embedding infrastructure: {}", 
                error.getMessage(), error))
        .onErrorReturn(false);
    }

    /**
     * Enrich search results with additional metadata
     * 
     * @param results Raw ChromaDB search results
     * @return Enriched results with parsed metadata
     */
    private List<Map<String, Object>> enrichSearchResults(List<Map<String, Object>> results) {
        return results.stream()
                .peek(result -> {
                    // Parse metadata
                    @SuppressWarnings("unchecked")
                    Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                    if (metadata != null) {
                        result.put("videoId", metadata.get("video_id"));
                        result.put("startTime", metadata.get("start_time"));
                        result.put("endTime", metadata.get("end_time"));
                        result.put("speaker", metadata.get("speaker"));
                        result.put("topic", metadata.get("topic"));
                    }
                })
                .toList();
    }

    /**
     * Enrich pipeline result with additional statistics
     * 
     * @param result Base result map
     * @param videoId Video ID
     * @return Enriched result with stats
     */
    private Map<String, Object> enrichResultWithStats(Map<String, Object> result, Long videoId) {
        Map<String, Object> enriched = new HashMap<>(result);
        
        // Get video info
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video != null) {
            enriched.put("videoTitle", video.getTitle());
            enriched.put("videoDurationSeconds", video.getDurationSeconds());
        }

        return enriched;
    }
}
