package com.aivideoip.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration for non-blocking HTTP calls
 * Used by OllamaClient for async communication with Ollama service
 * 
 * Features:
 * - Connection pooling
 * - Timeout handling
 * - Memory buffer optimization
 * - Retry capabilities
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WebClientConfig {

    @Value("${app.ollama.timeout:120}")
    private long timeout;

    /**
     * Configure WebClient for Ollama API calls
     * 
     * Features:
     * - Connection pool with max 50 connections
     * - Read/Write timeouts of 120 seconds (configurable)
     * - 16MB buffer for large responses
     * - Default headers (Content-Type, Accept)
     * 
     * @return configured WebClient.Builder
     */
    @Bean
    public WebClient webClient() {
        log.info("Configuring WebClient with timeout: {} seconds", timeout);

        // Connection pool configuration
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ollama-pool")
                .maxConnections(50)
                .pendingAcquireTimeout(java.time.Duration.ofSeconds(60))
                .pendingAcquireMaxCount(100)
                .build();

        // HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) (timeout * 1000))
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(java.time.Duration.ofSeconds(timeout))
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(timeout, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(timeout, TimeUnit.SECONDS))
                );

        // Exchange strategies for buffer size
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Alternative WebClient builder for custom configurations
     * 
     * @return WebClient.Builder for custom setup
     */
    @Bean(name = "ollamaWebClientBuilder")
    public WebClient.Builder ollamaWebClientBuilder() {
        log.debug("Creating Ollama WebClient builder");

        ConnectionProvider connectionProvider = ConnectionProvider.builder("ollama-builder")
                .maxConnections(50)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(java.time.Duration.ofSeconds(timeout));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
    
}
