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
 * <p>Updated for Phase 11 (2026-11-19): the endpoint list now covers
 * all 43 POST trace endpoints (Phase 11's astar, rbtree/insert,
 * rbtree/search, dp/lcs and dp/coinchange).</p>
 *
 * <p>Updated for Phase 12 (2026-11-19): the endpoint list now covers
 * all 48 POST trace endpoints (Phase 12's rbtree/delete,
 * graph/astarweighted, graph/floydwarshall, dp/editdistance and
 * dp/lps).</p>
 *
 * <p>Updated for Phase 13 (2026-11-19): the endpoint list now covers
 * all 54 POST trace endpoints (Phase 13's segmenttree/build,
 * segmenttree/query, segmenttree/update, unionfind/union,
 * string/kmp and backtracking/nqueens).</p>
 *
 * <p>Updated for Phase 15 (2026-11-19): the endpoint list now covers
 * all 59 POST trace endpoints (Phase 15's sort/bucket, dp/lis,
 * dp/mcm, string/rabinkarp and trie/delete).</p>
 *
 * <p>Updated for Phase 16 (2026-11-19): the endpoint list now covers
 * all 65 POST trace endpoints (Phase 16's sort/quickselect,
 * fenwick/build, fenwick/prefix, fenwick/update,
 * string/boyermoore and string/zalgorithm).</p>
 *
 * <p>Updated for Phase 17 (2026-11-19): the endpoint list now covers
 * all 68 POST trace endpoints (Phase 17's lru/simulate,
 * btree/insert and hash/bloom).</p>
 *
 * <p>Updated for Phase 18 (2026-11-19): the endpoint list now covers
 * all 72 POST trace endpoints (Phase 18's btree/search,
 * btree/delete, string/suffixarray and skiplist/insert).</p>
 *
 * <p>Updated for Phase 19 (2026-11-20): the endpoint list now covers
 * all 75 POST trace endpoints (Phase 19's hash/probing,
 * graph/scc and string/huffman).</p>
 *
 * <p>Updated for Phase 20 (2026-11-20): the endpoint list now covers
 * all 78 POST trace endpoints (Phase 20's graph/maxflow,
 * lfu/simulate and backtracking/sudoku).</p>
 *
 * <p>Updated for Phase 21 (2026-11-20): the endpoint list now covers
 * all 81 POST trace endpoints (Phase 21's string/ahocorasick,
 * graph/bridges and string/manacher).</p>
 *
 * <p>Updated for Phase 22 (2026-11-20): the endpoint list now covers
 * all 84 POST trace endpoints (Phase 22's graph/bidijkstra,
 * graph/ch and geo/geohash — the Google Maps algorithms).</p>
 *
 * <p>Updated for Phase 23 (2026-11-20): the endpoint list now covers
 * all 90 POST trace endpoints (Phase 23's spatial/quadtree,
 * spatial/kdtree, spatial/rtree, graph/yen, graph/traffic and
 * spatial/tsp — graphs and Google Maps, part 2).</p>
 *
 * <p>Updated for Phase 24 (2026-11-20): the endpoint list now covers
 * all 97 POST trace endpoints (Phase 24's geometry/convexhull,
 * geometry/closestpair, geometry/linesweep, geometry/voronoi,
 * search/invertedindex, search/tfidf and search/pagerank —
 * computational geometry and search &amp; ranking).</p>
 *
 * <p>Updated for Phase 25 (2026-11-20): the endpoint list now covers
 * all 103 POST trace endpoints (Phase 25's tree/hld,
 * tree/centroid, tree/persistentseg, flow/hopcroftkarp,
 * flow/mincostflow and flow/hungarian — advanced trees and
 * matching &amp; flow).</p>
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
                        "POST /api/sort/bucket",
                        "POST /api/sort/quickselect",
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
                        "POST /api/hash/bloom",
                        "POST /api/hash/probing",
                        "POST /api/lru/simulate",
                        "POST /api/lfu/simulate",
                        "POST /api/btree/insert",
                        "POST /api/btree/search",
                        "POST /api/btree/delete",
                        "POST /api/graph/bfs",
                        "POST /api/graph/dfs",
                        "POST /api/graph/dijkstra",
                        "POST /api/graph/prim",
                        "POST /api/graph/kruskal",
                        "POST /api/graph/bellmanford",
                        "POST /api/graph/toposort",
                        "POST /api/graph/astar",
                        "POST /api/graph/astarweighted",
                        "POST /api/graph/floydwarshall",
                        "POST /api/graph/scc",
                        "POST /api/graph/maxflow",
                        "POST /api/flow/hopcroftkarp",
                        "POST /api/flow/mincostflow",
                        "POST /api/flow/hungarian",
                        "POST /api/graph/bridges",
                        "POST /api/graph/bidijkstra",
                        "POST /api/graph/ch",
                        "POST /api/geo/geohash",
                        "POST /api/spatial/quadtree",
                        "POST /api/spatial/kdtree",
                        "POST /api/spatial/rtree",
                        "POST /api/graph/yen",
                        "POST /api/graph/traffic",
                        "POST /api/spatial/tsp",
                        "POST /api/geometry/convexhull",
                        "POST /api/geometry/closestpair",
                        "POST /api/geometry/linesweep",
                        "POST /api/geometry/voronoi",
                        "POST /api/search/invertedindex",
                        "POST /api/search/tfidf",
                        "POST /api/search/pagerank",
                        "POST /api/tree/hld",
                        "POST /api/tree/centroid",
                        "POST /api/tree/persistentseg",
                        "POST /api/dp/knapsack",
                        "POST /api/dp/lcs",
                        "POST /api/dp/coinchange",
                        "POST /api/dp/editdistance",
                        "POST /api/dp/lps",
                        "POST /api/dp/lis",
                        "POST /api/dp/mcm",
                        "POST /api/avl/insert",
                        "POST /api/avl/search",
                        "POST /api/avl/delete",
                        "POST /api/rbtree/insert",
                        "POST /api/rbtree/search",
                        "POST /api/rbtree/delete",
                        "POST /api/trie/insert",
                        "POST /api/trie/search",
                        "POST /api/trie/delete",
                        "POST /api/segmenttree/build",
                        "POST /api/segmenttree/query",
                        "POST /api/segmenttree/update",
                        "POST /api/fenwick/build",
                        "POST /api/fenwick/prefix",
                        "POST /api/fenwick/update",
                        "POST /api/range/sqrtdecomp",
                        "POST /api/range/mos",
                        "POST /api/range/sparsetable",
                        "POST /api/unionfind/union",
                        "POST /api/string/kmp",
                        "POST /api/string/rabinkarp",
                        "POST /api/string/boyermoore",
                        "POST /api/string/zalgorithm",
                        "POST /api/string/suffixarray",
                        "POST /api/string/huffman",
                        "POST /api/string/ahocorasick",
                        "POST /api/string/manacher",
                        "POST /api/string/sam",
                        "POST /api/string/eertree",
                        "POST /api/string/lcpkasai",
                        "POST /api/string/ahocorasick2",
                        "POST /api/string/suffixtrie",
                        "POST /api/string/samlcs",
                        "POST /api/range/lazyseg",
                        "POST /api/range/pstkth",
                        "POST /api/range/sqrtdecomp2",
                        "POST /api/backtracking/nqueens",
                        "POST /api/backtracking/sudoku",
                        "POST /api/skiplist/insert"
                )
        );
    }
}
