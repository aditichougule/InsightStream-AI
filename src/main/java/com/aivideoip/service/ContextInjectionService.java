package com.aivideoip.service;

import com.aivideoip.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Step 22: Context Injection Service
 * 
 * Implements advanced context injection strategies to improve RAG quality:
 * - Proper context formatting with clear boundaries
 * - Instruction injection to enforce context-only answers
 * - Timestamp awareness for accurate references
 * - Handling of missing context scenarios
 */
@Service
@Slf4j
public class ContextInjectionService {
    
    /**
     * Build enhanced prompt with context injection
     * 
     * Ensures LLM:
     * 1. Answers ONLY from provided context
     * 2. Rejects out-of-context questions
     * 3. Includes timestamps in responses
     * 4. Maintains accuracy and specificity
     */
    public String buildContextInjectedPrompt(String question, String context, String videoTitle, boolean hasContext) {
        if (!hasContext || context.trim().isEmpty()) {
            return buildNoContextPrompt(question, videoTitle);
        }
        
        return String.format(
                "You are answering questions about the video: \"%s\"\n\n" +
                        "TRANSCRIPT CONTEXT:\n" +
                        "==================\n" +
                        "%s\n" +
                        "==================\n\n" +
                        "CRITICAL INSTRUCTIONS:\n" +
                        "1. Answer ONLY using the transcript context provided above\n" +
                        "2. If the answer cannot be found in the context, respond EXACTLY with: \"Not found in video.\"\n" +
                        "3. Reference timestamps [HH:MM:SS] from context when relevant\n" +
                        "4. Be concise, accurate, and grounded in the provided text\n" +
                        "5. Do NOT make assumptions, inferences, or add external knowledge\n" +
                        "6. Do NOT say the information is 'not in the provided context' - use only \"Not found in video.\"\n\n" +
                        "QUESTION: %s\n\n" +
                        "ANSWER (grounded in context only):",
                videoTitle,
                context,
                question
        );
    }
    
    /**
     * Build prompt when no context is available
     */
    private String buildNoContextPrompt(String question, String videoTitle) {
        return String.format(
                "You are answering questions about the video: \"%s\"\n\n" +
                        "Unfortunately, no relevant transcript context was found.\n\n" +
                        "QUESTION: %s\n\n" +
                        "RESPONSE:",
                videoTitle,
                question
        );
    }
    
    /**
     * Format context with clear boundaries and timestamps
     * 
     * Step 22 Enhancement: Improved context formatting
     */
    public String formatContextWithBoundaries(List<ChatResponse.ChatSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < sources.size(); i++) {
            ChatResponse.ChatSource source = sources.get(i);
            
            context.append(String.format(
                    "[CHUNK %d] [%s - %s]\n%s\n\n",
                    i + 1,
                    source.getStartTime(),
                    source.getEndTime(),
                    source.getText()
            ));
        }
        
        return context.toString();
    }
    
    /**
     * Build system prompt for context-aware behavior
     * 
     * Can be used as a system message in multi-turn conversations
     */
    public String buildSystemPrompt() {
        return "You are a precise question-answering assistant that ONLY uses provided context.\n" +
                "When asked a question:\n" +
                "1. Search the provided context carefully\n" +
                "2. If found, answer with relevant details and timestamps\n" +
                "3. If NOT found, respond EXACTLY with: \"Not found in video.\"\n" +
                "4. Never add information from your training data\n" +
                "5. Never speculate or infer beyond what's explicitly stated";
    }
    
    /**
     * Validate context quality and coverage
     * 
     * Returns quality metrics for the retrieved context
     */
    public ContextQualityMetrics evaluateContextQuality(List<ChatResponse.ChatSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return ContextQualityMetrics.builder()
                    .hasContext(false)
                    .chunkCount(0)
                    .averageSimilarity(0f)
                    .totalTextLength(0)
                    .isHighQuality(false)
                    .build();
        }
        
        float avgSimilarity = (float) sources.stream()
                .mapToDouble(s -> s.getSimilarityScore() != null ? s.getSimilarityScore() : 0f)
                .average()
                .orElse(0f);
        
        int totalLength = sources.stream()
                .mapToInt(s -> s.getText() != null ? s.getText().length() : 0)
                .sum();
        
        boolean isHighQuality = avgSimilarity >= 0.7f && totalLength >= 500;
        
        log.debug("Context quality: chunks={}, avgSimilarity={}, textLength={}, quality={}",
                sources.size(), avgSimilarity, totalLength, isHighQuality ? "HIGH" : "MEDIUM");
        
        return ContextQualityMetrics.builder()
                .hasContext(true)
                .chunkCount(sources.size())
                .averageSimilarity(avgSimilarity)
                .totalTextLength(totalLength)
                .isHighQuality(isHighQuality)
                .build();
    }
    
    /**
     * Post-process LLM response to ensure it respects context boundaries
     * 
     * Sanitizes responses that violate context-only constraints
     */
    public String sanitizeResponse(String response, List<ChatResponse.ChatSource> sources) {
        if (response == null || response.isBlank()) {
            return "Not found in video.";
        }
        
        String trimmed = response.trim();
        
        // Check if response is the "not found" answer
        if (trimmed.equalsIgnoreCase("Not found in video.") || 
            trimmed.equalsIgnoreCase("I don't know.") ||
            trimmed.equalsIgnoreCase("Not found.")) {
            return "Not found in video.";
        }
        
        // Log when response indicates information was not in context
        if (trimmed.contains("not provided") || trimmed.contains("not mentioned") ||
            trimmed.contains("not in the context") || trimmed.contains("beyond the provided")) {
            log.debug("Response indicates information not in context: {}", trimmed);
            return "Not found in video.";
        }
        
        return trimmed;
    }
    
    /**
     * Metrics for context quality evaluation
     */
    public static class ContextQualityMetrics {
        private boolean hasContext;
        private int chunkCount;
        private float averageSimilarity;
        private int totalTextLength;
        private boolean isHighQuality;
        
        public ContextQualityMetrics(boolean hasContext, int chunkCount, float averageSimilarity, 
                                     int totalTextLength, boolean isHighQuality) {
            this.hasContext = hasContext;
            this.chunkCount = chunkCount;
            this.averageSimilarity = averageSimilarity;
            this.totalTextLength = totalTextLength;
            this.isHighQuality = isHighQuality;
        }
        
        public static ContextQualityMetricsBuilder builder() {
            return new ContextQualityMetricsBuilder();
        }
        
        public boolean isHasContext() { return hasContext; }
        public int getChunkCount() { return chunkCount; }
        public float getAverageSimilarity() { return averageSimilarity; }
        public int getTotalTextLength() { return totalTextLength; }
        public boolean isHighQuality() { return isHighQuality; }
        
        public static class ContextQualityMetricsBuilder {
            private boolean hasContext;
            private int chunkCount;
            private float averageSimilarity;
            private int totalTextLength;
            private boolean isHighQuality;
            
            public ContextQualityMetricsBuilder hasContext(boolean hasContext) {
                this.hasContext = hasContext;
                return this;
            }
            
            public ContextQualityMetricsBuilder chunkCount(int chunkCount) {
                this.chunkCount = chunkCount;
                return this;
            }
            
            public ContextQualityMetricsBuilder averageSimilarity(float averageSimilarity) {
                this.averageSimilarity = averageSimilarity;
                return this;
            }
            
            public ContextQualityMetricsBuilder totalTextLength(int totalTextLength) {
                this.totalTextLength = totalTextLength;
                return this;
            }
            
            public ContextQualityMetricsBuilder isHighQuality(boolean isHighQuality) {
                this.isHighQuality = isHighQuality;
                return this;
            }
            
            public ContextQualityMetrics build() {
                return new ContextQualityMetrics(hasContext, chunkCount, averageSimilarity, 
                        totalTextLength, isHighQuality);
            }
        }
    }
}
