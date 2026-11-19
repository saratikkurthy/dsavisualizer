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
 * <p>Updated for Phase 4 (2026-11-18): the endpoint list now covers
 * all 26 POST trace endpoints, not just the five Phase 1 ones.</p>
 *
 * <p>Updated for Phase 8 (2026-11-18): the endpoint list now covers
 * all 35 POST trace endpoints (Phase 7's heapsort, dijkstra,
 * bst/delete and avl/delete plus Phase 8's prim, kruskal,
 * trie/insert, trie/search and shell).</p>
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
                        "POST /api/sort/merge",
                        "POST /api/sort/quick",
                        "POST /api/sort/counting",
                        "POST /api/sort/radix",
                        "POST /api/sort/heapsort",
                        "POST /api/sort/shell",
                        "POST /api/search/linear",
                        "POST /api/search/binary",
                        "POST /api/stack/push",
                        "POST /api/stack/pop",
                        "POST /api/queue/enqueue",
                        "POST /api/queue/dequeue",
                        "POST /api/list/insert",
                        "POST /api/list/delete",
                        "POST /api/list/search",
                        "POST /api/bst/insert",
                        "POST /api/bst/search",
                        "POST /api/bst/delete",
                        "POST /api/heap/insert",
                        "POST /api/heap/extract-min",
                        "POST /api/hash/insert",
                        "POST /api/hash/search",
                        "POST /api/graph/bfs",
                        "POST /api/graph/dfs",
                        "POST /api/graph/dijkstra",
                        "POST /api/graph/prim",
                        "POST /api/graph/kruskal",
                        "POST /api/avl/insert",
                        "POST /api/avl/search",
                        "POST /api/avl/delete",
                        "POST /api/trie/insert",
                        "POST /api/trie/search"
                )
        );
    }
}
