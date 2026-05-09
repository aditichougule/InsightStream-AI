package com.aivideoip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for AI Video Intelligence Platform
 */
@SpringBootApplication
@EnableScheduling
public class AiVideoIntelligencePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiVideoIntelligencePlatformApplication.class, args);
    }
}
