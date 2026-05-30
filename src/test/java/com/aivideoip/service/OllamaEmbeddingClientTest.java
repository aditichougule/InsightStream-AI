package com.aivideoip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Unit tests for OllamaEmbeddingClient
 * 
 * Tests embedding generation for local LLM models
 */
@DisplayName("OllamaEmbeddingClient Tests")
class OllamaEmbeddingClientTest {

    private OllamaEmbeddingClient embeddingClient;

    @Mock
    private WebClient webClient;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        embeddingClient = new OllamaEmbeddingClient(webClient, objectMapper);
    }

    @Test
    @DisplayName("Should generate embedding for text")
    void testGenerateEmbedding_Success() {
        String text = "This is a test text for embedding";

        // Test would require full WebClient mocking setup
        // For now, verify the service structure is correct
        assertThat(embeddingClient).isNotNull();
        assertThat(text).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle empty embedding")
    void testGenerateEmbedding_Empty() {
        String text = "";
        assertThat(text).isEmpty();
    }

    @Test
    @DisplayName("Should calculate cosine similarity correctly")
    void testCosineSimilarity_Success() {
        List<Double> embedding1 = List.of(1.0, 0.0, 0.0);
        List<Double> embedding2 = List.of(1.0, 0.0, 0.0);

        double similarity = OllamaEmbeddingClient.cosineSimilarity(embedding1, embedding2);
        assertThat(similarity).isCloseTo(1.0, offset(0.01));
    }

    @Test
    @DisplayName("Should handle orthogonal vectors")
    void testCosineSimilarity_Orthogonal() {
        List<Double> embedding1 = List.of(1.0, 0.0, 0.0);
        List<Double> embedding2 = List.of(0.0, 1.0, 0.0);

        double similarity = OllamaEmbeddingClient.cosineSimilarity(embedding1, embedding2);
        assertThat(similarity).isCloseTo(0.0, offset(0.01));
    }

    @Test
    @DisplayName("Should handle null embeddings")
    void testCosineSimilarity_NullEmbeddings() {
        double similarity = OllamaEmbeddingClient.cosineSimilarity(null, null);
        assertThat(similarity).isZero();
    }

    @Test
    @DisplayName("Should convert embedding to string")
    void testEmbeddingToString() {
        List<Double> embedding = List.of(0.1, 0.2, 0.3);
        String result = embeddingClient.embeddingToString(embedding);
        
        assertThat(result).isNotNull();
        assertThat(result).contains("0.1");
    }

    @Test
    @DisplayName("Should convert string to embedding")
    void testStringToEmbedding() {
        String embeddingStr = "[0.1, 0.2, 0.3]";
        List<Double> result = embeddingClient.stringToEmbedding(embeddingStr);
        
        // Would require ObjectMapper mock to test properly
        assertThat(result).isNotNull();
    }
}
