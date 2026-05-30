package com.aivideoip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Service for managing vector embeddings in ChromaDB
 * 
 * ChromaDB provides:
 * - Persistent vector storage with metadata
 * - Similarity search capabilities
 * - Easy integration with embeddings
 * - Local or remote deployment options
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChromaDBClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${chromadb.api.url:http://localhost:8000}")
    private String chromadbApiUrl;

    @Value("${chromadb.collection.name:video-embeddings}")
    private String collectionName;

    private static final String COLLECTION_ENDPOINT = "/api/v1/collections";
    private static final String ADD_ENDPOINT = "/api/v1/collections/{name}/add";
    private static final String QUERY_ENDPOINT = "/api/v1/collections/{name}/query";
    private static final String DELETE_ENDPOINT = "/api/v1/collections/{name}/delete";

    /**
     * Initialize or get a collection in ChromaDB
     * 
     * @return Collection metadata
     */
    public Mono<Boolean> initializeCollection() {
        log.info("Initializing ChromaDB collection: {}", collectionName);

        Map<String, Object> collectionMetadata = new HashMap<>();
        collectionMetadata.put("name", collectionName);
        collectionMetadata.put("metadata", Map.of("hnsw:space", "cosine"));

        return webClient.post()
                .uri(chromadbApiUrl + COLLECTION_ENDPOINT)
                .bodyValue(collectionMetadata)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.warn("Collection may already exist or error occurred: {}", error.getMessage()))
                .map(response -> true)
                .onErrorReturn(true);  // Collection might already exist, that's ok
    }

    /**
     * Add embeddings to ChromaDB collection
     * 
     * @param chunkId Unique identifier for the chunk
     * @param videoId Video ID for grouping
     * @param text Original text of the chunk
     * @param embedding Vector embedding
     * @param metadata Additional metadata (startTime, endTime, speaker, etc.)
     * @return true if successful
     */
    public Mono<Boolean> addEmbedding(String chunkId, Long videoId, String text, 
                                     List<Double> embedding, Map<String, Object> metadata) {
        log.debug("Adding embedding to ChromaDB for chunk: {}", chunkId);

        if (embedding == null || embedding.isEmpty()) {
            log.warn("Empty embedding for chunk: {}", chunkId);
            return Mono.just(false);
        }

        Map<String, Object> addRequest = new HashMap<>();
        
        // ChromaDB expects these fields
        addRequest.put("ids", List.of(chunkId));
        addRequest.put("embeddings", List.of(embedding));
        addRequest.put("documents", List.of(text));
        
        // Add metadata
        Map<String, Object> fullMetadata = new HashMap<>(metadata != null ? metadata : new HashMap<>());
        fullMetadata.put("video_id", videoId.toString());
        fullMetadata.put("chunk_id", chunkId);
        addRequest.put("metadatas", List.of(fullMetadata));

        return webClient.post()
                .uri(chromadbApiUrl + ADD_ENDPOINT, collectionName)
                .bodyValue(addRequest)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("Successfully added embedding for chunk: {}", chunkId);
                    return true;
                })
                .doOnError(error -> log.error("Failed to add embedding for chunk: {}, error: {}", 
                        chunkId, error.getMessage(), error))
                .onErrorReturn(false);
    }

    /**
     * Query ChromaDB for similar embeddings
     * 
     * @param queryEmbedding The embedding to search for
     * @param videoId Filter results to specific video
     * @param topK Number of results to return
     * @return List of similar chunks with scores
     */
    public Mono<List<Map<String, Object>>> querySimilar(List<Double> queryEmbedding, 
                                                        Long videoId, int topK) {
        log.debug("Querying ChromaDB for similar embeddings, top {}", topK);

        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            log.warn("Empty query embedding");
            return Mono.just(List.of());
        }

        Map<String, Object> queryRequest = new HashMap<>();
        queryRequest.put("query_embeddings", List.of(queryEmbedding));
        queryRequest.put("n_results", Math.min(topK, 100));
        
        // Filter by video_id if provided
        if (videoId != null) {
            Map<String, Object> whereFilter = new HashMap<>();
            whereFilter.put("video_id", videoId.toString());
            queryRequest.put("where", whereFilter);
        }

        return webClient.post()
                .uri(chromadbApiUrl + QUERY_ENDPOINT, collectionName)
                .bodyValue(queryRequest)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> parseQueryResults(response))
                .doOnError(error -> log.error("Failed to query ChromaDB: {}", error.getMessage(), error))
                .onErrorReturn(List.of());
    }

    /**
     * Query ChromaDB for embeddings by text (generates embedding internally)
     * 
     * @param queryText Text to search for similar chunks
     * @param embeddingClient Embedding client to generate query embedding
     * @param videoId Filter to specific video
     * @param topK Number of results
     * @return List of similar results
     */
    public Mono<List<Map<String, Object>>> querySimilarByText(String queryText, 
                                                               OllamaEmbeddingClient embeddingClient,
                                                               Long videoId, int topK) {
        log.debug("Querying ChromaDB with text query");

        return embeddingClient.generateEmbedding(queryText)
                .flatMap(embedding -> querySimilar(embedding, videoId, topK));
    }

    /**
     * Delete embeddings for a video from ChromaDB
     * 
     * @param videoId Video ID to delete embeddings for
     * @return true if successful
     */
    public Mono<Boolean> deleteVideoEmbeddings(Long videoId) {
        log.info("Deleting embeddings for video: {}", videoId);

        Map<String, Object> deleteRequest = new HashMap<>();
        Map<String, Object> whereFilter = new HashMap<>();
        whereFilter.put("video_id", videoId.toString());
        deleteRequest.put("where", whereFilter);

        return webClient.post()
                .uri(chromadbApiUrl + DELETE_ENDPOINT, collectionName)
                .bodyValue(deleteRequest)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("Successfully deleted embeddings for video: {}", videoId);
                    return true;
                })
                .doOnError(error -> log.error("Failed to delete embeddings for video: {}, error: {}", 
                        videoId, error.getMessage(), error))
                .onErrorReturn(false);
    }

    /**
     * Get collection statistics
     * 
     * @return Collection metadata and stats
     */
    public Mono<Map<String, Object>> getCollectionStats() {
        log.debug("Fetching collection statistics");

        return webClient.get()
                .uri(chromadbApiUrl + COLLECTION_ENDPOINT + "/{name}", collectionName)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> stats = (Map<String, Object>) objectMapper.readValue(response, Map.class);
                        return Mono.<Map<String, Object>>just(stats);
                    } catch (Exception e) {
                        log.error("Failed to parse collection stats: {}", e.getMessage(), e);
                        return Mono.<Map<String, Object>>just(new HashMap<>());
                    }
                })
                .doOnError(error -> log.error("Failed to get collection stats: {}", error.getMessage(), error))
                .onErrorReturn(new HashMap<>());
    }

    /**
     * Check if ChromaDB is available and healthy
     * 
     * @return true if ChromaDB is responding
     */
    public Mono<Boolean> healthCheck() {
        log.debug("Checking ChromaDB health");

        return webClient.get()
                .uri(chromadbApiUrl + "/api/v1/heartbeat")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("ChromaDB is healthy");
                    return true;
                })
                .doOnError(error -> log.warn("ChromaDB health check failed: {}", error.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * Parse query results from ChromaDB response
     * 
     * @param jsonResponse Raw JSON response from ChromaDB
     * @return List of result maps with ids, documents, distances, and metadatas
     */
    private Mono<List<Map<String, Object>>> parseQueryResults(String jsonResponse) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = objectMapper.readValue(jsonResponse, Map.class);
            
            @SuppressWarnings("unchecked")
            List<List<String>> ids = (List<List<String>>) response.get("ids");
            @SuppressWarnings("unchecked")
            List<List<String>> documents = (List<List<String>>) response.get("documents");
            @SuppressWarnings("unchecked")
            List<List<Double>> distances = (List<List<Double>>) response.get("distances");
            @SuppressWarnings("unchecked")
            List<List<Map<String, Object>>> metadatas = (List<List<Map<String, Object>>>) response.get("metadatas");

            List<Map<String, Object>> results = new ArrayList<>();

            if (ids != null && !ids.isEmpty() && !ids.get(0).isEmpty()) {
                List<String> firstQueryIds = ids.get(0);
                List<String> firstQueryDocs = documents != null && !documents.isEmpty() ? documents.get(0) : new ArrayList<>();
                List<Double> firstQueryDistances = distances != null && !distances.isEmpty() ? distances.get(0) : new ArrayList<>();
                List<Map<String, Object>> firstQueryMetadatas = metadatas != null && !metadatas.isEmpty() ? metadatas.get(0) : new ArrayList<>();

                for (int i = 0; i < firstQueryIds.size(); i++) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", firstQueryIds.get(i));
                    result.put("document", i < firstQueryDocs.size() ? firstQueryDocs.get(i) : "");
                    result.put("distance", i < firstQueryDistances.size() ? firstQueryDistances.get(i) : 1.0);
                    result.put("similarity", i < firstQueryDistances.size() ? 1.0 - firstQueryDistances.get(i) : 0.0);
                    result.put("metadata", i < firstQueryMetadatas.size() ? firstQueryMetadatas.get(i) : new HashMap<>());
                    
                    results.add(result);
                }
            }

            log.debug("Parsed {} results from ChromaDB query", results.size());
            return Mono.just(results);
        } catch (Exception e) {
            log.error("Failed to parse ChromaDB query results: {}", e.getMessage(), e);
            return Mono.just(List.of());
        }
    }
}
