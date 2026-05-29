package com.aivideoip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OllamaClient
 * Tests WebClient integration with Ollama API
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaClient Tests")
class OllamaClientTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OllamaClient ollamaClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // We'll use reflection or constructor to inject webClient if possible
        ollamaClient = new OllamaClient(webClient);
    }

    @Test
    @DisplayName("Should generate text successfully")
    void testGenerateText_Success() {
        // Given
        String prompt = "What is artificial intelligence?";
        String expectedResponse = "Artificial intelligence is the simulation of human intelligence...";

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", expectedResponse);

        // Mock the entire chain
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(objectMapper.writeValueAsString(responseBody)));

        // When
        Mono<String> result = ollamaClient.generateText(prompt);

        // Then
        assertNotNull(result);
        result.as(StepVerifier::create)
                .assertNext(response -> assertTrue(response.contains("Artificial intelligence")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should generate text with specific model")
    void testGenerateText_WithModel() {
        // Given
        String prompt = "Explain machine learning";
        String modelName = "mistral";
        String expectedResponse = "Machine learning is a subset of AI...";

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", expectedResponse);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(objectMapper.writeValueAsString(responseBody)));

        // When
        Mono<String> result = ollamaClient.generateText(prompt, modelName);

        // Then
        assertNotNull(result);
        StepVerifier.create(result)
                .assertNext(response -> assertTrue(response.contains("Machine learning")))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should generate text synchronously")
    void testGenerateTextSync_Success() {
        // Given
        String prompt = "What is deep learning?";
        String expectedResponse = "Deep learning uses multiple layers of neural networks...";

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("response", expectedResponse);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(objectMapper.writeValueAsString(responseBody)));

        // When
        String result = ollamaClient.generateTextSync(prompt);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Deep learning"));
    }

    @Test
    @DisplayName("Should generate embeddings successfully")
    void testGenerateEmbedding_Success() {
        // Given
        String text = "This is a test document";
        List<Double> expectedEmbedding = List.of(0.1, 0.2, 0.3, 0.4, 0.5);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("embedding", expectedEmbedding);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(objectMapper.writeValueAsString(responseBody)));

        // When
        Mono<List<Double>> result = ollamaClient.generateEmbedding(text);

        // Then
        assertNotNull(result);
        StepVerifier.create(result)
                .assertNext(embedding -> {
                    assertEquals(5, embedding.size());
                    assertEquals(0.1, embedding.get(0));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should list available models")
    void testListModels_Success() {
        // Given
        String modelName1 = "llama3";
        String modelName2 = "mistral";

        Map<String, Object> responseBody = new HashMap<>();
        Map<String, Object> models = new HashMap<>();
        Map<String, String> model1 = Map.of("name", modelName1);
        Map<String, String> model2 = Map.of("name", modelName2);
        models.put("models", List.of(model1, model2));

        when(webClient.get()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(objectMapper.writeValueAsString(models)));

        // When
        Mono<List<Map<String, Object>>> result = ollamaClient.listModels();

        // Then
        assertNotNull(result);
        StepVerifier.create(result)
                .assertNext(modelList -> {
                    assertTrue(modelList.size() > 0);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should perform health check")
    void testHealthCheck_Success() {
        // Given
        when(webClient.get()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(Mono.just(new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.OK)));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            ollamaClient.healthCheckSync();
        });
    }

    @Test
    @DisplayName("Should handle health check asynchronously")
    void testHealthCheckAsync() {
        // Given
        when(webClient.get()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.just(new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.OK)));

        // When
        Mono<Boolean> result = ollamaClient.healthCheck();

        // Then
        assertNotNull(result);
        StepVerifier.create(result)
                .assertNext(health -> assertTrue(health))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle API error with 4xx status")
    void testGenerateText_ClientError() {
        // Given
        String prompt = "Test prompt";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        WebClientResponseException exception = new WebClientResponseException(
                400, "Bad Request", null, null, null
        );

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(exception));

        // When & Then
        Mono<String> result = ollamaClient.generateText(prompt);
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle API error with 5xx status")
    void testGenerateText_ServerError() {
        // Given
        String prompt = "Test prompt";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        WebClientResponseException exception = new WebClientResponseException(
                503, "Service Unavailable", null, null, null
        );

        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(exception));

        // When & Then
        Mono<String> result = ollamaClient.generateText(prompt);
        StepVerifier.create(result)
                .expectError(WebClientResponseException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle timeout gracefully")
    void testGenerateText_Timeout() {
        // Given
        String prompt = "Test prompt";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new java.util.concurrent.TimeoutException("Request timeout")));

        // When & Then
        Mono<String> result = ollamaClient.generateText(prompt);
        StepVerifier.create(result)
                .expectError(java.util.concurrent.TimeoutException.class)
                .verify();
    }

    @Test
    @DisplayName("Should handle null response gracefully")
    void testGenerateText_NullResponse() {
        // Given
        String prompt = "Test prompt";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.empty());

        // When & Then
        Mono<String> result = ollamaClient.generateText(prompt);
        StepVerifier.create(result)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("Should create valid request body for text generation")
    void testRequestBodyConstruction() {
        // Given
        String prompt = "Test prompt";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just("{\"response\": \"result\"}"));

        // When
        ollamaClient.generateText(prompt).block();

        // Then - Verify bodyValue was called
        verify(requestBodySpec, times(1)).bodyValue(argThat(body ->
                body instanceof Map &&
                ((Map<?, ?>) body).containsKey("prompt")
        ));
    }
}
