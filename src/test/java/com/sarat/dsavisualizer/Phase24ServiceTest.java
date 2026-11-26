package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.ClosestPairService;
import com.sarat.dsavisualizer.service.ConvexHullService;
import com.sarat.dsavisualizer.service.InvertedIndexService;
import com.sarat.dsavisualizer.service.LineSweepService;
import com.sarat.dsavisualizer.service.PageRankService;
import com.sarat.dsavisualizer.service.TfidfService;
import com.sarat.dsavisualizer.service.VoronoiService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 24 service tests. Convex hull tests check the demo
 * (A(1,1) B(2,4) C(4,6) D(6,5) E(7,2) F(5,1) G(3,3) H(4,2):
 * sorted A, F, E, H, D, G, C, B; hull A, F, E, D, C, B (6);
 * pops H when D arrives, G when C arrives; 10 steps).
 * Closest pair tests check the demo (the K-d set: left best
 * 3.162, right best 1.414, strip 2.828 rejected, answer
 * 1.414 pair (7,2)-(8,1), 9 steps) and a 3-point case.
 * Line sweep tests check the demo (S1 (1,1)-(6,6), S2
 * (1,6)-(6,1), S3 (2,0)-(2,7): 3 crossings, 10 steps) and
 * a no-crossing case. Voronoi tests check the demo (sites
 * A(2,2) B(8,2) C(5,8) D(5,5): 12 clip steps, areas sum to
 * 100, 13 steps) and a 2-site case. Inverted index tests
 * check the demo corpus (query "tree traversal" &rarr;
 * [D2, D5], 9 steps), a single-term query and a no-match
 * query. TF-IDF tests check the demo query ("binary tree":
 * D1 0.5973 &gt; D2 0.4479 &gt; D5 0.1733, 3 steps) and the
 * second query ("graph traversal"). PageRank tests check
 * the demo graph (10 iterations: C 0.3745 &gt; A 0.3496
 * &gt; B 0.1761, 11 steps), a 1-iteration run, and the
 * no-page-count default (5 pages). The 400 tests call the
 * controller endpoints directly — the controller is built
 * with nulls for the other services because the validators
 * 400 before any service is touched (the Phase 16
 * precedent).
 */
class Phase24ServiceTest {

    private final ConvexHullService convexHullService =
            new ConvexHullService();
    private final ClosestPairService closestPairService =
            new ClosestPairService();
    private final LineSweepService lineSweepService =
            new LineSweepService();
    private final VoronoiService voronoiService = new VoronoiService();
    private final InvertedIndexService invertedIndexService =
            new InvertedIndexService();
    private final TfidfService tfidfService = new TfidfService();
    private final PageRankService pageRankService =
            new PageRankService();

    /** The convex-hull demo points (Phase 24 design §A1). */
    private static final String HULL_POINTS =
            "1,1;2,4;4,6;6,5;7,2;5,1;3,3;4,2";

    /** The closest-pair demo points (Phase 24 design §A2). */
    private static final String PAIR_POINTS = "2,3;5,4;9,6;4,7;8,1;7,2";

    /** The line-sweep demo segments (Phase 24 design §A3). */
    private static final String SWEEP_SEGMENTS =
            "1,1,6,6;1,6,6,1;2,0,2,7";

    /** The Voronoi demo sites (Phase 24 design §A4). */
    private static final String VORONOI_SITES = "2,2;8,2;5,8;5,5";

    /** The demo corpus (Phase 24 design §A5/§A6). */
    private static final List<String> CORPUS = List.of(
            "binary tree search",
            "binary search tree traversal",
            "graph shortest path",
            "hash table lookup",
            "tree traversal preorder inorder",
            "graph traversal breadth depth");

    /** The PageRank demo links (Phase 24 design §A7). */
    private static final List<List<Integer>> PAGERANK_EDGES = List.of(
            List.of(0, 1), List.of(0, 2), List.of(1, 2),
            List.of(2, 0), List.of(3, 2), List.of(4, 3),
            List.of(4, 2));

    /** The controller under test: nulls for every service except
     * the Phase 24 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 44
     * Phase 1–22 params, 6 Phase 23 params, then convexhull,
     * closestpair, linesweep, voronoi, invertedindex, tfidf,
     * pagerank (57 total). */
    

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    /** A request with only the word set. */
    private TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    /** A request with words (docs) + word (query) set. */
    private TraceRequest wordsRequest(List<String> words, String word) {
        TraceRequest request = new TraceRequest();
        request.setWords(words);
        request.setWord(word);
        return request;
    }

    /** A PageRank request: graphEdges + iterations (index) + page count (value). */
    private TraceRequest pageRankRequest(List<List<Integer>> edges,
                                         Integer iterations,
                                         Integer pageCount) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setIndex(iterations);
        request.setValue(pageCount);
        return request;
    }

    /** The line numbers of a trace, in order. */
    private List<Integer> lines(List<Step> steps) {
        return steps.stream().map(Step::getLine).toList();
    }

    /** The node values of a step, in node order. */
    private List<Integer> nodeValues(Step step) {
        return step.getNodes().stream()
                .map(n -> n.getValue()).toList();
    }

    /** The edges of a step as "from-to" strings, in order. */
    private List<String> edgePairs(Step step) {
        return step.getEdges().stream()
                .map(e -> e.getFrom() + "-" + e.getTo()).toList();
    }

    @Test
    void convexHullDemo() {
        List<Step> steps = convexHullService.convexHull(HULL_POINTS);
        assertEquals(10, steps.size());
        assertEquals(List.of(1, 2, 2, 2, 2, 2, 2, 2, 2, 0),
                lines(steps));
        // Hull sizes 1, 2, 3, 4, 4 (pop H), 5, 5 (pop G), 6.
        assertEquals(List.of(1), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(2).getData());
        assertEquals(List.of(3), steps.get(3).getData());
        assertEquals(List.of(4), steps.get(4).getData());
        assertEquals(List.of(4), steps.get(5).getData());
        assertEquals(List.of(5), steps.get(6).getData());
        assertEquals(List.of(5), steps.get(7).getData());
        assertEquals(List.of(6), steps.get(8).getData());
        assertEquals(List.of(6), steps.get(9).getData());
        // Pops: H (index 7) when D arrives, G (index 6) when C arrives.
        assertEquals(List.of(7), steps.get(5).getSwapped());
        assertEquals(List.of(6), steps.get(7).getSwapped());
        assertEquals(List.of(), steps.get(4).getSwapped());
        assertEquals(List.of(), steps.get(8).getSwapped());
        // Highlights: the point being processed (input index).
        assertEquals(List.of(0), steps.get(1).getHighlight());
        assertEquals(List.of(5), steps.get(2).getHighlight());
        assertEquals(List.of(4), steps.get(3).getHighlight());
        assertEquals(List.of(7), steps.get(4).getHighlight());
        assertEquals(List.of(3), steps.get(5).getHighlight());
        assertEquals(List.of(6), steps.get(6).getHighlight());
        assertEquals(List.of(2), steps.get(7).getHighlight());
        assertEquals(List.of(1), steps.get(8).getHighlight());
        // Final hull closed: A-F-E-D-C-B (indices 0-5-4-3-2-1).
        assertEquals(List.of("0-5", "5-4", "4-3", "3-2", "2-1", "1-0"),
                edgePairs(steps.get(9)));
        // Final node values: hull points 1, G and H 0.
        assertEquals(List.of(1, 1, 1, 1, 1, 1, 0, 0),
                nodeValues(steps.get(9)));
        assertEquals("Convex hull: A, F, E, D, C, B (6 points).",
                steps.get(9).getMessage());
    }

    @Test
    void convexHullSortStep() {
        List<Step> steps = convexHullService.convexHull(HULL_POINTS);
        assertEquals(1, steps.get(0).getLine());
        assertEquals(List.of(), steps.get(0).getData());
        assertTrue(steps.get(0).getMessage().contains(
                "A, F, E, H, D, G, C, B"));
    }

    @Test
    void convexHullTooFewPoints() {
        List<Step> steps = convexHullService.convexHull("1,1;2,4");
        assertEquals(1, steps.size());
        assertEquals(0, steps.get(0).getLine());
        assertEquals(List.of(), steps.get(0).getData());
    }

    @Test
    void closestPairDemo() {
        List<Step> steps = closestPairService.closestPair(PAIR_POINTS);
        assertEquals(9, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 2, 2, 3, 4, 0),
                lines(steps));
        // Best distance so far: 4472, 3162, 3162, 1414, then 1414.
        assertEquals(List.of(4472), steps.get(0).getData());
        assertEquals(List.of(3162), steps.get(1).getData());
        assertEquals(List.of(3162), steps.get(2).getData());
        assertEquals(List.of(1414), steps.get(3).getData());
        assertEquals(List.of(1414), steps.get(6).getData());
        assertEquals(List.of(1414), steps.get(7).getData());
        assertEquals(List.of(1414), steps.get(8).getData());
        // Highlights in comparison order (x-sorted, then y-sorted strip).
        assertEquals(List.of(0, 3), steps.get(0).getHighlight());
        assertEquals(List.of(0, 1), steps.get(1).getHighlight());
        assertEquals(List.of(3, 1), steps.get(2).getHighlight());
        assertEquals(List.of(5, 4), steps.get(3).getHighlight());
        assertEquals(List.of(5, 2), steps.get(4).getHighlight());
        assertEquals(List.of(4, 2), steps.get(5).getHighlight());
        assertEquals(List.of(5, 1), steps.get(6).getHighlight());
        assertEquals(List.of(4, 5), steps.get(7).getHighlight());
        // Sorted: best pair so far, lower-first.
        assertEquals(List.of(0, 3), steps.get(0).getSorted());
        assertEquals(List.of(0, 1), steps.get(1).getSorted());
        assertEquals(List.of(0, 1), steps.get(2).getSorted());
        assertEquals(List.of(4, 5), steps.get(3).getSorted());
        assertEquals(List.of(4, 5), steps.get(6).getSorted());
        assertEquals(List.of(4, 5), steps.get(7).getSorted());
        assertEquals(List.of(4, 5), steps.get(8).getSorted());
        // Node values: 0 left / 1 right of the split at x=6.
        assertEquals(List.of(0, 0, 1, 0, 1, 1),
                nodeValues(steps.get(0)));
        // Final: answer 1.414, pair (7,2)-(8,1).
        assertEquals(List.of(1414), steps.get(8).getData());
        assertEquals(List.of(4, 5), steps.get(8).getSorted());
        assertEquals(List.of("4-5"), edgePairs(steps.get(8)));
        assertEquals("Closest pair: F(7,2)-E(8,1), distance 1.414.",
                steps.get(8).getMessage());
    }

    @Test
    void closestPairThreePoints() {
        List<Step> steps = closestPairService.closestPair("2,3;5,4;9,6");
        assertEquals(4, steps.size());
        assertEquals(List.of(2, 2, 2, 0), lines(steps));
        assertEquals(List.of(3162), steps.get(0).getData());
        assertEquals(List.of(3162), steps.get(1).getData());
        assertEquals(List.of(3162), steps.get(2).getData());
        assertEquals(List.of(3162), steps.get(3).getData());
        assertEquals(List.of(0, 1), steps.get(3).getSorted());
        assertEquals(List.of(0, 0, 0), nodeValues(steps.get(0)));
    }

    @Test
    void lineSweepDemo() {
        List<Step> steps = lineSweepService.lineSweep(SWEEP_SEGMENTS);
        assertEquals(10, steps.size());
        assertEquals(List.of(2, 2, 2, 3, 3, 2, 3, 2, 2, 0),
                lines(steps));
        // Intersections found: 0, 0, 0, 1, 2, 2, 3, 3, 3, 3.
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(0), steps.get(2).getData());
        assertEquals(List.of(1), steps.get(3).getData());
        assertEquals(List.of(2), steps.get(4).getData());
        assertEquals(List.of(2), steps.get(5).getData());
        assertEquals(List.of(3), steps.get(6).getData());
        assertEquals(List.of(3), steps.get(9).getData());
        // Active sets after each event.
        assertEquals(List.of(0), steps.get(0).getSorted());
        assertEquals(List.of(0, 1), steps.get(1).getSorted());
        assertEquals(List.of(2, 0, 1), steps.get(2).getSorted());
        assertEquals(List.of(0, 2, 1), steps.get(3).getSorted());
        assertEquals(List.of(0, 1, 2), steps.get(4).getSorted());
        assertEquals(List.of(0, 1), steps.get(5).getSorted());
        assertEquals(List.of(1, 0), steps.get(6).getSorted());
        assertEquals(List.of(0), steps.get(7).getSorted());
        assertEquals(List.of(), steps.get(8).getSorted());
        assertEquals(List.of(), steps.get(9).getSorted());
        // Highlights: endpoint node ids, or segment indices on a crossing.
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(4), steps.get(2).getHighlight());
        assertEquals(List.of(0, 2), steps.get(3).getHighlight());
        assertEquals(List.of(1, 2), steps.get(4).getHighlight());
        assertEquals(List.of(5), steps.get(5).getHighlight());
        assertEquals(List.of(0, 1), steps.get(6).getHighlight());
        assertEquals(List.of(3), steps.get(7).getHighlight());
        assertEquals(List.of(1), steps.get(8).getHighlight());
        // The 3 segments as edges.
        assertEquals(List.of("0-1", "2-3", "4-5"),
                edgePairs(steps.get(0)));
        assertEquals("3 intersections: S1×S3 at (2,2), S2×S3 at "
                        + "(2,5), S1×S2 at (3.5,3.5).",
                steps.get(9).getMessage());
    }

    @Test
    void lineSweepNoCrossing() {
        List<Step> steps = lineSweepService.lineSweep("0,0,5,0;0,2,5,2");
        assertEquals(5, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 0), lines(steps));
        assertEquals(List.of(0), steps.get(4).getData());
        assertEquals(List.of(0, 1), steps.get(1).getSorted());
        assertEquals(List.of(), steps.get(4).getSorted());
        assertEquals("0 intersections.", steps.get(4).getMessage());
    }

    @Test
    void voronoiDemo() {
        List<Step> steps = voronoiService.voronoi(VORONOI_SITES);
        assertEquals(13, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0),
                lines(steps));
        // The 12 clip polygons, in hundredths (canonicalised).
        assertEquals(List.of(0, 0, 500, 0, 500, 1000, 0, 1000),
                steps.get(0).getData());
        assertEquals(List.of(0, 0, 500, 0, 500, 425, 0, 675),
                steps.get(1).getData());
        assertEquals(List.of(0, 0, 500, 0, 500, 200, 50, 650, 0, 675),
                steps.get(2).getData());
        assertEquals(List.of(500, 0, 1000, 0, 1000, 1000, 500, 1000),
                steps.get(3).getData());
        assertEquals(List.of(500, 0, 1000, 0, 1000, 675, 500, 425),
                steps.get(4).getData());
        assertEquals(List.of(500, 0, 1000, 0, 1000, 675, 950, 650,
                500, 200), steps.get(5).getData());
        assertEquals(List.of(0, 675, 1000, 175, 1000, 1000, 0, 1000),
                steps.get(6).getData());
        assertEquals(List.of(0, 675, 500, 425, 1000, 675, 1000, 1000,
                0, 1000), steps.get(7).getData());
        assertEquals(List.of(0, 675, 50, 650, 950, 650, 1000, 675,
                1000, 1000, 0, 1000), steps.get(8).getData());
        assertEquals(List.of(0, 700, 700, 0, 1000, 0, 1000, 1000,
                0, 1000), steps.get(9).getData());
        assertEquals(List.of(0, 700, 500, 200, 1000, 700, 1000, 1000,
                0, 1000), steps.get(10).getData());
        assertEquals(List.of(50, 650, 500, 200, 950, 650),
                steps.get(11).getData());
        // Highlights: the site being clipped.
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(1), steps.get(3).getHighlight());
        assertEquals(List.of(2), steps.get(6).getHighlight());
        assertEquals(List.of(3), steps.get(9).getHighlight());
        // Node values: 1 for the site being clipped.
        assertEquals(List.of(1, 0, 0, 0), nodeValues(steps.get(0)));
        assertEquals(List.of(0, 0, 0, 1), nodeValues(steps.get(11)));
        assertEquals(List.of(), steps.get(12).getData());
        assertTrue(steps.get(12).getMessage().contains("Cells:"));
        assertTrue(steps.get(12).getMessage().contains(
                "A (0,0),(5,0),(5,2),(0.5,6.5),(0,6.75)"));
        assertTrue(steps.get(12).getMessage().contains(
                "D (0.5,6.5),(5,2),(9.5,6.5)"));
    }

    @Test
    void voronoiTwoSites() {
        List<Step> steps = voronoiService.voronoi("2,5;8,5");
        assertEquals(3, steps.size());
        assertEquals(List.of(2, 2, 0), lines(steps));
        assertEquals(List.of(0, 0, 500, 0, 500, 1000, 0, 1000),
                steps.get(0).getData());
        assertEquals(List.of(500, 0, 1000, 0, 1000, 1000, 500, 1000),
                steps.get(1).getData());
        assertEquals(List.of(), steps.get(2).getData());
    }

    @Test
    void invertedIndexDemo() {
        List<Step> steps = invertedIndexService.invertedIndex(
                CORPUS, "tree traversal");
        assertEquals(9, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 2, 2, 3, 3, 0),
                lines(steps));
        // Build steps: one doc per step.
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(5), steps.get(5).getData());
        assertEquals(List.of(1, 1, 1, 1, 1, 1),
                nodeValues(steps.get(5)));
        // Query steps: "tree" -> [D1,D2,D5], "traversal" -> [D2,D5,D6].
        assertEquals(List.of(0, 1, 4), steps.get(6).getData());
        assertEquals(List.of(0, 1, 4), steps.get(6).getHighlight());
        assertEquals(List.of(1, 4, 5), steps.get(7).getData());
        assertEquals(List.of(1, 4, 5), steps.get(7).getHighlight());
        assertEquals(List.of(0, 1, 0, 0, 1, 1),
                nodeValues(steps.get(7)));
        // Final: AND -> [D2, D5].
        assertEquals(List.of(1, 4), steps.get(8).getData());
        assertEquals(List.of(1, 4), steps.get(8).getHighlight());
        assertEquals(List.of(0, 1, 0, 0, 1, 0),
                nodeValues(steps.get(8)));
        assertEquals("Query 'tree traversal' (AND): [D2, D5].",
                steps.get(8).getMessage());
    }

    @Test
    void invertedIndexDefaultQuery() {
        List<Step> steps = invertedIndexService.invertedIndex(
                CORPUS, null);
        assertEquals(9, steps.size());
        assertEquals(List.of(1, 4), steps.get(8).getData());
    }

    @Test
    void invertedIndexSingleTerm() {
        List<Step> steps = invertedIndexService.invertedIndex(
                CORPUS, "graph");
        assertEquals(8, steps.size());
        assertEquals(List.of(2, 5), steps.get(6).getData());
        assertEquals(List.of(2, 5), steps.get(7).getData());
    }

    @Test
    void invertedIndexNoMatch() {
        List<Step> steps = invertedIndexService.invertedIndex(
                CORPUS, "quantum");
        assertEquals(8, steps.size());
        assertEquals(List.of(), steps.get(6).getData());
        assertEquals(List.of(), steps.get(7).getData());
        assertEquals("Query 'quantum' (AND): no matches.",
                steps.get(7).getMessage());
    }

    @Test
    void tfidfDemo() {
        List<Step> steps = tfidfService.tfidf(CORPUS, "binary tree");
        assertEquals(3, steps.size());
        assertEquals(List.of(2, 2, 0), lines(steps));
        // After "binary": D1 0.366, D2 0.275.
        assertEquals(List.of(366, 275, 0, 0, 0, 0),
                steps.get(0).getData());
        assertEquals(List.of(0, 1), steps.get(0).getHighlight());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(0).getSorted());
        // After "tree": D1 0.597, D2 0.448, D5 0.173.
        assertEquals(List.of(597, 448, 0, 0, 173, 0),
                steps.get(1).getData());
        assertEquals(List.of(0, 1, 4), steps.get(1).getHighlight());
        assertEquals(List.of(0, 1, 4, 2, 3, 5),
                steps.get(1).getSorted());
        assertEquals(List.of(1, 1, 0, 0, 1, 0),
                nodeValues(steps.get(1)));
        // Final: D1 > D2 > D5 > D3 = D4 = D6.
        assertEquals(List.of(597, 448, 0, 0, 173, 0),
                steps.get(2).getData());
        assertEquals(List.of(0, 1, 4, 2, 3, 5),
                steps.get(2).getSorted());
        assertTrue(steps.get(2).getMessage().contains(
                "D1 > D2 > D5 > D3 = D4 = D6"));
    }

    @Test
    void tfidfSecondQuery() {
        List<Step> steps = tfidfService.tfidf(CORPUS, "graph traversal");
        assertEquals(3, steps.size());
        // After "graph": D3 0.366, D6 0.275.
        assertEquals(List.of(0, 0, 366, 0, 0, 275),
                steps.get(0).getData());
        assertEquals(List.of(2, 5), steps.get(0).getHighlight());
        // After "traversal": D6 0.448, D3 0.366, D2 = D5 0.173.
        assertEquals(List.of(0, 173, 366, 0, 173, 448),
                steps.get(1).getData());
        assertEquals(List.of(5, 2, 1, 4, 0, 3),
                steps.get(1).getSorted());
        assertEquals(List.of(0, 173, 366, 0, 173, 448),
                steps.get(2).getData());
        assertEquals(List.of(5, 2, 1, 4, 0, 3),
                steps.get(2).getSorted());
    }

    @Test
    void pagerankDemo() {
        List<Step> steps = pageRankService.pagerank(
                PAGERANK_EDGES, 10, 6);
        assertEquals(11, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 0),
                lines(steps));
        // Iteration 1 (spot check) and iteration 2.
        assertEquals(List.of(190, 119, 474, 119, 49, 49),
                steps.get(0).getData());
        assertEquals(List.of(434, 113, 336, 53, 32, 32),
                steps.get(1).getData());
        // Largest-change page per iteration.
        assertEquals(List.of(2), steps.get(0).getHighlight());
        assertEquals(List.of(0), steps.get(1).getHighlight());
        assertEquals(List.of(0), steps.get(2).getHighlight());
        assertEquals(List.of(1), steps.get(3).getHighlight());
        assertEquals(List.of(2), steps.get(4).getHighlight());
        assertEquals(List.of(0), steps.get(5).getHighlight());
        assertEquals(List.of(0), steps.get(6).getHighlight());
        assertEquals(List.of(1), steps.get(7).getHighlight());
        assertEquals(List.of(2), steps.get(8).getHighlight());
        assertEquals(List.of(0), steps.get(9).getHighlight());
        // Iteration 5 ranking flips to A > C.
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                steps.get(4).getSorted());
        // Iteration 1 node values: rank positions C=1, A=2, B=3, D=4, E=5, F=6.
        assertEquals(List.of(2, 3, 1, 4, 5, 6),
                nodeValues(steps.get(0)));
        // Final: C 0.375, A 0.350, B 0.176, D 0.042, E 0.029, F 0.029.
        assertEquals(List.of(350, 176, 375, 42, 29, 29),
                steps.get(10).getData());
        assertEquals(List.of(2, 0, 1, 3, 4, 5),
                steps.get(10).getSorted());
        assertEquals(List.of(2), steps.get(10).getHighlight());
        assertEquals(List.of(2, 3, 1, 4, 5, 6),
                nodeValues(steps.get(10)));
        assertEquals(List.of("0-1", "0-2", "1-2", "2-0", "3-2",
                "4-3", "4-2"), edgePairs(steps.get(10)));
        assertTrue(steps.get(10).getMessage().contains(
                "C > A > B > D > E = F"));
    }

    @Test
    void pagerankOneIteration() {
        List<Step> steps = pageRankService.pagerank(
                PAGERANK_EDGES, 1, 6);
        assertEquals(2, steps.size());
        assertEquals(List.of(2, 0), lines(steps));
        assertEquals(List.of(190, 119, 474, 119, 49, 49),
                steps.get(0).getData());
        assertEquals(List.of(2, 0, 1, 3, 4, 5),
                steps.get(0).getSorted());
        assertEquals(List.of(190, 119, 474, 119, 49, 49),
                steps.get(1).getData());
    }

    @Test
    void pagerankNoPageCount() {
        // No page count: N defaults to max-node-id + 1 = 5 (F dropped).
        List<Step> steps = pageRankService.pagerank(
                PAGERANK_EDGES, 10, null);
        assertEquals(11, steps.size());
        assertEquals(List.of(200, 115, 540, 115, 30),
                steps.get(0).getData());
        assertEquals(List.of(361, 181, 385, 43, 30),
                steps.get(10).getData());
        assertEquals(List.of(2, 0, 1, 3, 4),
                steps.get(10).getSorted());
        assertEquals(5, steps.get(10).getNodes().size());
    }


}
