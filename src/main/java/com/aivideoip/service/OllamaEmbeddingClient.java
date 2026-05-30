package com.aivideoip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for generating embeddings using Ollama's nomic-embed-text model
 * 
 * Local embeddings provide:
 * - Privacy: No data sent to external APIs
 * - Cost: Free, runs on local hardware
 * - Speed: Fast inference on GPU (if available)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaEmbeddingClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ollama.api.url:http://localhost:11434}")
    private String ollamaApiUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String embeddingModel;

    /**
     * Generate embedding for a text chunk using Ollama
     * 
     * @param text The text to embed
     * @return A list of floats representing the embedding vector
     */
    public Mono<List<Double>> generateEmbedding(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());

        Map<String, Object> request = new HashMap<>();
        request.put("model", embeddingModel);
        request.put("prompt", text);

        return webClient.post()
                .uri(ollamaApiUrl + "/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Error calling Ollama embeddings API: {}", error.getMessage(), error))
                .flatMap(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Double> embedding = (List<Double>) responseMap.get("embedding");
                        
                        if (embedding == null || embedding.isEmpty()) {
                            log.warn("Empty embedding received from Ollama");
                            return Mono.empty();
                        }
                        
                        log.debug("Successfully generated embedding with dimension: {}", embedding.size());
                        return Mono.just(embedding);
                    } catch (Exception e) {
                        log.error("Failed to parse Ollama embeddings response: {}", e.getMessage(), e);
                        return Mono.error(e);
                    }
                })
                .doOnNext(embedding -> log.trace("Embedding vector size: {}", embedding.size()));
    }

    /**
     * Generate embeddings for multiple text chunks (batch operation)
     * 
     * @param texts List of texts to embed
     * @return A map of text to embedding vectors
     */
    public Mono<Map<String, List<Double>>> generateEmbeddingsBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts in batch", texts.size());

        if (texts == null || texts.isEmpty()) {
            log.warn("Empty text list provided for batch embedding");
            return Mono.just(new HashMap<>());
        }

        // Process embeddings sequentially to avoid overwhelming the model
        return Flux.fromIterable(texts)
                .concatMap(text -> generateEmbedding(text)
                        .map(embedding -> Map.entry(text, embedding))
                        .onErrorReturn(Map.entry(text, List.of()))
                )
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .doOnNext(result -> log.info("Batch embedding complete: {} of {} texts processed successfully",
                        result.values().stream().filter(v -> !v.isEmpty()).count(),
                        texts.size()));
    }

    /**
     * Verify that the embedding model is available in Ollama
     * 
     * @return true if model is ready, false otherwise
     */
    public Mono<Boolean> isModelAvailable() {
        log.info("Checking if embedding model '{}' is available in Ollama", embeddingModel);

        return webClient.get()
                .uri(ollamaApiUrl + "/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> models = (List<Map<String, Object>>) responseMap.get("models");
                        
                        if (models != null) {
                            boolean available = models.stream()
                                    .anyMatch(model -> embeddingModel.equals(model.get("name")));
                            log.info("Model '{}' available: {}", embeddingModel, available);
                            return available;
                        }
                        return false;
                    } catch (Exception e) {
                        log.error("Failed to parse Ollama tags response: {}", e.getMessage(), e);
                        return false;
                    }
                })
                .doOnError(error -> log.error("Error checking model availability: {}", error.getMessage(), error))
                .onErrorReturn(false);
    }

    /**
     * Convert embedding vector to a string representation for storage
     * 
     * @param embedding The embedding vector
     * @return JSON string representation
     */
    public String embeddingToString(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception e) {
            log.error("Error converting embedding to string: {}", e.getMessage(), e);
            return "[]";
        }
    }

    /**
     * Convert string representation back to embedding vector
     * 
     * @param embeddingStr JSON string representation
     * @return List of doubles
     */
    public List<Double> stringToEmbedding(String embeddingStr) {
        try {
            @SuppressWarnings("unchecked")
            List<Double> embedding = objectMapper.readValue(embeddingStr, List.class);
            return embedding;
        } catch (Exception e) {
            log.error("Error converting string to embedding: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Calculate cosine similarity between two embedding vectors
     * 
     * @param embedding1 First embedding vector
     * @param embedding2 Second embedding vector
     * @return Similarity score between 0 and 1
     */
    public static double cosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.isEmpty() || embedding2.isEmpty() ||
            embedding1.size() != embedding2.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            double val1 = embedding1.get(i);
            double val2 = embedding2.get(i);
            
            dotProduct += val1 * val2;
            norm1 += val1 * val1;
            norm2 += val2 * val2;
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
