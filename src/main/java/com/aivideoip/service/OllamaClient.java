package com.aivideoip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * WebClient-based HTTP client for Ollama REST API
 * Provides non-blocking async communication with local LLM models
 * 
 * Supports:
 * - Text generation
 * - Streaming responses (optional)
 * - Model listing
 * - Service health checks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ollama.enabled:true}")
    private boolean ollamaEnabled;

    @Value("${app.ollama.api-url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${app.ollama.model:llama3}")
    private String model;

    @Value("${app.ollama.max-tokens:2000}")
    private int maxTokens;

    @Value("${app.ollama.temperature:0.7}")
    private double temperature;

    @Value("${app.ollama.timeout:120}")
    private long timeout;

    /**
     * Generate text using Ollama model
     * Non-blocking async call with WebClient
     *
     * @param prompt the input prompt
     * @return Mono containing generated text response
     */
    public Mono<String> generateText(String prompt) {
        if (!ollamaEnabled) {
            log.warn("Ollama service is disabled");
            return Mono.just("Ollama service is disabled");
        }

        return generateText(prompt, model);
    }

    /**
     * Generate text using specified model
     * Non-blocking async call with WebClient
     *
     * @param prompt the input prompt
     * @param modelName the model to use
     * @return Mono containing generated text response
     */
    public Mono<String> generateText(String prompt, String modelName) {
        if (!ollamaEnabled) {
            log.warn("Ollama service is disabled");
            return Mono.just("Ollama service is disabled");
        }

        log.debug("Generating text with model: {} | Prompt length: {} chars", modelName, prompt.length());

        Map<String, Object> requestBody = buildGenerateRequest(prompt, modelName);

        return webClient.post()
                .uri(ollamaUrl + "/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> extractResponse(response))
                .doOnError(error -> log.error("Error calling Ollama API: {}", error.getMessage(), error))
                .timeout(java.time.Duration.ofSeconds(timeout))
                .onErrorResume(error -> {
                    log.error("Ollama request failed after {} seconds", timeout);
                    return Mono.just("Error: " + error.getMessage());
                });
    }

    /**
     * Generate embeddings for text
     * Useful for semantic search and RAG
     *
     * @param text the text to embed
     * @return Mono containing embedding vector
     */
    public Mono<java.util.List<Double>> generateEmbedding(String text) {
        if (!ollamaEnabled) {
            log.warn("Ollama service is disabled");
            return Mono.just(new java.util.ArrayList<>());
        }

        log.debug("Generating embedding for text: {} chars", text.length());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", text);

        return webClient.post()
                .uri(ollamaUrl + "/api/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (java.util.List<Double>) (java.util.List<?>) extractEmbedding(response))
                .doOnError(error -> log.error("Error generating embedding: {}", error.getMessage()))
                .onErrorResume(error -> Mono.just(new java.util.ArrayList<>()));
    }

    /**
     * List all available models in Ollama
     *
     * @return Mono containing list of available models
     */
    public Mono<java.util.List<Map<String, Object>>> listModels() {
        log.debug("Fetching available models from Ollama");

        return webClient.get()
                .uri(ollamaUrl + "/api/tags")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (java.util.List<Map<String, Object>>) (java.util.List<?>) extractModels(response))
                .doOnError(error -> log.error("Error fetching models: {}", error.getMessage()))
                .onErrorResume(error -> Mono.just(new java.util.ArrayList<>()));
    }

    /**
     * Check health of Ollama service
     *
     * @return Mono<Boolean> true if service is healthy
     */
    public Mono<Boolean> healthCheck() {
        log.debug("Checking Ollama service health at: {}", ollamaUrl);

        return webClient.get()
                .uri(ollamaUrl + "/api/tags")
                .retrieve()
                .toBodilessEntity()
                .map(response -> {
                    log.info("Ollama service is healthy");
                    return true;
                })
                .doOnError(error -> log.error("Ollama health check failed: {}", error.getMessage()))
                .onErrorResume(error -> Mono.just(false));
    }

    /**
     * Generate text synchronously (blocking call)
     * Useful for methods that require synchronous behavior
     *
     * @param prompt the input prompt
     * @return generated text
     */
    public String generateTextSync(String prompt) {
        log.debug("Generating text synchronously");
        return generateText(prompt)
                .block(java.time.Duration.ofSeconds(timeout));
    }

    /**
     * Generate text with custom model synchronously
     *
     * @param prompt the input prompt
     * @param modelName the model to use
     * @return generated text
     */
    public String generateTextSync(String prompt, String modelName) {
        log.debug("Generating text synchronously with model: {}", modelName);
        return generateText(prompt, modelName)
                .block(java.time.Duration.ofSeconds(timeout));
    }

    /**
     * List models synchronously
     *
     * @return list of available models
     */
    public java.util.List<Map<String, Object>> listModelsSync() {
        log.debug("Listing models synchronously");
        return listModels()
                .block(java.time.Duration.ofSeconds(10));
    }

    /**
     * Health check synchronously
     *
     * @return true if service is healthy
     */
    public boolean healthCheckSync() {
        log.debug("Health check synchronously");
        Boolean result = healthCheck()
                .block(java.time.Duration.ofSeconds(10));
        return result != null && result;
    }

    // ============= Private Helper Methods =============

    private Map<String, Object> buildGenerateRequest(String prompt, String modelName) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", modelName);
        request.put("prompt", prompt);
        request.put("stream", false);
        request.put("temperature", temperature);
        request.put("num_predict", maxTokens);
        
        // Optional: system prompt for better responses
        request.put("system", "You are a helpful AI assistant. Provide clear, concise, and accurate responses.");
        
        return request;
    }

    private String extractResponse(Map<String, Object> response) {
        if (response == null) {
            log.error("Null response from Ollama API");
            return "";
        }

        Object responseObj = response.get("response");
        if (responseObj == null) {
            log.error("No 'response' field in Ollama response: {}", response.keySet());
            return "";
        }

        String content = responseObj.toString().trim();
        log.debug("Extracted response: {} characters", content.length());
        return content;
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Double> extractEmbedding(Map<String, Object> response) {
        if (response == null) {
            log.error("Null response from embeddings API");
            return java.util.List.of();
        }

        Object embeddingObj = response.get("embedding");
        if (embeddingObj instanceof java.util.List) {
            return (java.util.List<Double>) embeddingObj;
        }

        log.error("Invalid embedding format in response");
        return java.util.List.of();
    }

    @SuppressWarnings("unchecked")
    private java.util.List<Map<String, Object>> extractModels(Map<String, Object> response) {
        if (response == null) {
            log.error("Null response from models API");
            return java.util.List.of();
        }

        Object modelsObj = response.get("models");
        if (modelsObj instanceof java.util.List) {
            return (java.util.List<Map<String, Object>>) modelsObj;
        }

        log.error("Invalid models format in response");
        return java.util.List.of();
    }

    /**
     * Get current configuration
     *
     * @return map of configuration values
     */
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", ollamaEnabled);
        config.put("apiUrl", ollamaUrl);
        config.put("model", model);
        config.put("maxTokens", maxTokens);
        config.put("temperature", temperature);
        config.put("timeout", timeout);
        return config;
    }
}
