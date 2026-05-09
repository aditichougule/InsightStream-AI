package com.aivideoip.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Video Intelligence Platform API")
                        .version("1.0.0")
                        .description("API for converting videos, lectures, podcasts into structured notes, summaries, and more")
                        .contact(new Contact()
                                .name("AI Video Intelligence Platform Team")
                                .url("https://github.com/yourusername/ai-video-intelligence-platform"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
