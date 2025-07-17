package com.example.argus.ocr.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI ocrOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("OCR API")
                        .description("Image to Text OCR API")
                        .version("1.0"));
    }
}

