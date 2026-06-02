package com.aivideoip.service;

import com.aivideoip.dto.ChatRequest;
import com.aivideoip.dto.ChatResponse;
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
 * Service implementing RAG (Retrieval-Augmented Generation) pipeline
 * 
 * Flow:
 * 1. Generate embedding for user question
 * 2. Search for similar transcript chunks in vector DB
 * 3. Retrieve relevant chunks with highest similarity
 * 4. Build context from retrieved chunks
 * 5. Send context + question to local LLM
 * 6. Return generated answer with source citations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RAGChatService {
    
    private final OllamaEmbeddingClient embeddingClient;
    private final OllamaClient ollamaClient;
    private final TranscriptChunkRepository chunkRepository;
    private final VideoRepository videoRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${app.ollama.embedding-model:nomic-embed-text}")
    private String embeddingModel;
    
    @Value("${app.ollama.chat-model:llama2}")
    private String chatModel;
    
    @Value("${app.rag.top-k:5}")
    private Integer defaultTopK;
    
    @Value("${app.rag.similarity-threshold:0.5}")
    private Float defaultThreshold;
    
    /**
     * Process chat query using RAG pipeline
     */
    @Transactional
    public ChatResponse processQuery(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing RAG query for video: {}, question: {}", 
                request.getVideoId(), request.getQuestion());
        
        try {
            // Step 1: Validate video exists
            Video video = videoRepository.findById(request.getVideoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
            
            // Step 2: Generate embedding for question
            List<Double> questionEmbedding = embeddingClient.generateEmbedding(
                    request.getQuestion()
            ).block();
            
            if (questionEmbedding == null || questionEmbedding.isEmpty()) {
                throw new RuntimeException("Failed to generate embedding for question");
            }
            
            log.debug("Generated embedding for question with {} dimensions", 
                    questionEmbedding.size());
            
            // Step 3: Search for similar chunks
            int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;
            Float threshold = request.getSimilarityThreshold() != null ? 
                    request.getSimilarityThreshold() : defaultThreshold;
            
            List<ChatResponse.ChatSource> sources = searchSimilarChunks(
                    request.getVideoId(),
                    questionEmbedding,
                    topK,
                    threshold
            );
            
            log.info("Retrieved {} relevant chunks", sources.size());
            
            if (sources.isEmpty()) {
                log.warn("No similar chunks found for question");
                return buildEmptyResponse(request.getQuestion(), startTime);
            }
            
            // Step 4: Build context from retrieved chunks
            String context = buildContext(sources);
            
            // Step 5: Generate answer using LLM with context
            String answer = generateAnswerWithContext(
                    request.getQuestion(),
                    context,
                    video.getTitle()
            );
            
            // Step 6: Calculate confidence based on similarity scores
            float confidence = sources.stream()
                    .map(ChatResponse.ChatSource::getSimilarityScore)
                    .reduce(0f, Float::sum) / sources.size();
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            log.info("Successfully processed query in {}ms with confidence: {}", 
                    processingTime, confidence);
            
            return ChatResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .confidence(confidence)
                    .generatedAt(System.currentTimeMillis())
                    .processingTimeMs(processingTime)
                    .build();
                    
        } catch (ResourceNotFoundException e) {
            log.warn("Resource not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing RAG query: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process chat query: " + e.getMessage());
        }
    }
    
    /**
     * Search for similar chunks using vector similarity
     */
    private List<ChatResponse.ChatSource> searchSimilarChunks(
            Long videoId,
            List<Double> embedding,
            int topK,
            Float threshold) {
        
        List<TranscriptChunk> chunks = chunkRepository.findByVideoIdOrderByStartTime(videoId);
        
        if (chunks.isEmpty()) {
            log.warn("No transcript chunks found for video: {}", videoId);
            return List.of();
        }
        
        List<ChunkSimilarity> similarities = chunks.stream()
                .map(chunk -> {
                    List<Double> chunkEmbedding = deserializeEmbedding(chunk.getEmbedding());
                    float similarity = calculateCosineSimilarity(
                            embedding,
                            chunkEmbedding
                    );
                    return new ChunkSimilarity(chunk, similarity);
                })
                .filter(cs -> cs.similarity >= threshold)
                .sorted(Comparator.comparing(ChunkSimilarity::getSimilarity).reversed())
                .limit(topK)
                .collect(Collectors.toList());
        
        log.debug("Found {} chunks with similarity >= {}", similarities.size(), threshold);
        
        return similarities.stream()
                .map(cs -> ChatResponse.ChatSource.builder()
                        .chunkId(cs.chunk.getId())
                        .text(cs.chunk.getChunkText())
                        .startSeconds(cs.chunk.getStartTime())
                        .endSeconds(cs.chunk.getEndTime())
                        .startTime(formatSeconds(cs.chunk.getStartTime()))
                        .endTime(formatSeconds(cs.chunk.getEndTime()))
                        .similarityScore(cs.similarity)
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * Calculate cosine similarity between two embedding vectors
     */
    private float calculateCosineSimilarity(List<Double> embedding1, List<Double> embedding2) {
        if (embedding1 == null || embedding2 == null || 
            embedding1.isEmpty() || embedding2.isEmpty()) {
            return 0f;
        }
        
        if (embedding1.size() != embedding2.size()) {
            log.warn("Embedding dimensions mismatch: {} vs {}", 
                    embedding1.size(), embedding2.size());
            return 0f;
        }
        
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < embedding1.size(); i++) {
            double a = embedding1.get(i);
            double b = embedding2.get(i);
            
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
     * Build context string from retrieved chunks
     */
    private String buildContext(List<ChatResponse.ChatSource> sources) {
        StringBuilder context = new StringBuilder();
        context.append("Context from video transcript:\n\n");
        
        for (ChatResponse.ChatSource source : sources) {
            context.append(String.format(
                    "[%s - %s] %s\n\n",
                    source.getStartTime(),
                    source.getEndTime(),
                    source.getText()
            ));
        }
        
        return context.toString();
    }
    
    /**
     * Generate answer using LLM with context
     */
    private String generateAnswerWithContext(String question, String context, String videoTitle) {
        String prompt = buildRAGPrompt(question, context, videoTitle);
        
        String answer = ollamaClient.generateText(prompt, chatModel).block();
        
        if (answer == null || answer.isBlank()) {
            log.warn("LLM returned empty answer for question: {}", question);
            return "Unable to generate answer from the video content.";
        }
        
        return answer.trim();
    }
    
    /**
     * Build the prompt for LLM with context and question
     */
    private String buildRAGPrompt(String question, String context, String videoTitle) {
        return String.format(
                "You are a helpful assistant answering questions about the video titled \"%s\".\n\n" +
                        "%s\n\n" +
                        "Based on the context above, please answer the following question:\n" +
                        "Question: %s\n\n" +
                        "Answer (be concise and accurate):",
                videoTitle,
                context,
                question
        );
    }
    
    /**
     * Build empty response when no relevant chunks found
     */
    private ChatResponse buildEmptyResponse(String question, long startTime) {
        return ChatResponse.builder()
                .answer("I could not find relevant information in the video transcript to answer your question.")
                .sources(List.of())
                .confidence(0f)
                .generatedAt(System.currentTimeMillis())
                .processingTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }
    
    /**
     * Format seconds to HH:MM:SS format
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
     * Inner class for chunk similarity comparison
     */
    private static class ChunkSimilarity {
        TranscriptChunk chunk;
        float similarity;
        
        ChunkSimilarity(TranscriptChunk chunk, float similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }
        
        public float getSimilarity() {
            return similarity;
        }
    }
    
    /**
     * Deserialize embedding string to List<Double>
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
}
