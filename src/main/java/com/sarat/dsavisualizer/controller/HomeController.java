package com.sarat.dsavisualizer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Root endpoint. The backend otherwise has no homepage, so opening
 * "/" used to return Spring Boot's Whitelabel 404 page. This returns
 * a small JSON index instead, with links to Swagger UI, the raw
 * OpenAPI docs, and the health check.
 *
 * <p>It also doubles as a build probe: if "/" returns this JSON,
 * the build live on Render includes the Swagger (springdoc) code.
 * If "/" still 404s, Render is still running the older build.</p>
 *
 * <p>No pom change needed — spring-boot-starter-web only.</p>
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home() {
        return Map.of(
                "service", "dsa-visualizer",
                "status", "ok",
                "swaggerUi", "/swagger-ui.html",
                "apiDocs", "/v3/api-docs",
                "health", "/api/health",
                "endpoints", List.of(
                        "POST /api/sort/bubble",
                        "POST /api/sort/selection",
                        "POST /api/sort/insertion",
                        "POST /api/search/linear",
                        "POST /api/search/binary"
                )
        );
    }
}
