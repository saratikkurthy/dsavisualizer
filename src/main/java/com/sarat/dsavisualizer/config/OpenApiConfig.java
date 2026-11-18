package com.sarat.dsavisualizer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI / OpenAPI metadata.
 *
 * Once the app is running, open:
 *   - Swagger UI:  /swagger-ui.html
 *   - Raw JSON docs: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dsaVisualizerApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("DSA Visualizer API")
                        .version("1.0")
                        .description("Visualgo-style step traces for sorting and "
                                + "search algorithms. Each endpoint takes an array "
                                + "of numbers and returns the step-by-step trace "
                                + "the React frontend plays back."));
    }
}
