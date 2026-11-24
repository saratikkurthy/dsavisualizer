package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.KdTreeService;
import com.sarat.dsavisualizer.service.QuadtreeService;
import com.sarat.dsavisualizer.service.RTreeService;
import com.sarat.dsavisualizer.service.TrafficDijkstraService;
import com.sarat.dsavisualizer.service.TspService;
import com.sarat.dsavisualizer.service.YenService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 23 service tests. Quadtree tests check the demo
 * (capacity 2: splits on C and F; NE query (50,50,100,100)
 * visits 9 nodes and returns [(55,55),(60,60),(80,80)], 16
 * steps; SW query returns [(10,10)], 12 steps). K-d tree tests
 * check the demo (query (9,2) &rarr; nearest (8,1), dist 1.414:
 * 4 visits + 1 prune + final = 6 steps) and a second query
 * (6,3) &rarr; (5,4), 1.414, 10 steps. R-tree tests check the
 * demo (M=4: root split into [R4,R3] / [R1,R2,R5], new root
 * (10,10,75,70); default query (12,12,30,30) &rarr; [R1,R2],
 * 13 steps) and the other two queries ((55,55,80,80) &rarr;
 * [R6], 13 steps; (0,0,100,100) &rarr; all 6, 15 steps).
 * Yen tests check the demo (K=3: 13 via [0,2,1,3,4,5], 14 via
 * [0,1,3,4,5], 14 via [0,2,1,3,5], 22 steps) and K=1/K=2.
 * Traffic tests check the demo (8:00 &rarr; [0,1,2,4,5],
 * length 9, 18 steps; 10:00 &rarr; [0,1,2,3,4,5], length 7,
 * 20 steps). TSP tests check the demo (NN tour A-B-F-C-E-D =
 * 312.6, swaps to 294.0 then 268.1, final A-B-D-E-F-C = 268.1,
 * 9 steps). The 400 tests call the controller endpoints
 * directly — the controller is built with nulls for the other
 * services because the validators 400 before any service is
 * touched (the Phase 16 precedent).
 */
class Phase23ServiceTest {

    private final QuadtreeService quadtreeService = new QuadtreeService();
    private final KdTreeService kdTreeService = new KdTreeService();
    private final RTreeService rTreeService = new RTreeService();
    private final YenService yenService = new YenService();
    private final TrafficDijkstraService trafficService =
            new TrafficDijkstraService();
    private final TspService tspService = new TspService();

    /** The quadtree demo points (Phase 23 design §A1). */
    private static final String QUAD_POINTS =
            "10,10;20,80;70,20;80,80;55,55;60,60";

    /** The k-d tree demo points (Phase 23 design §A2). */
    private static final String KD_POINTS = "2,3;5,4;9,6;4,7;8,1;7,2";

    /** The R-tree demo rects (Phase 23 design §A3). */
    private static final String RTREE_RECTS =
            "10,10,20,20;15,15,25,25;60,10,70,20;"
                    + "65,15,75,25;10,60,20,70;60,60,70,70";

    /** The Yen demo graph = the CH demo graph (Phase 23 design §B1). */
    private static final List<List<Integer>> YEN_EDGES = List.of(
            List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, 1),
            List.of(1, 3, 5), List.of(2, 3, 8), List.of(2, 4, 10),
            List.of(3, 4, 2), List.of(3, 5, 6), List.of(4, 5, 3));

    /** The traffic demo graph = the Phase 22 street graph (§B2). */
    private static final List<List<Integer>> TRAFFIC_EDGES = List.of(
            List.of(0, 1, 2), List.of(0, 2, 4), List.of(1, 2, 1),
            List.of(1, 3, 5), List.of(2, 3, 1), List.of(2, 4, 4),
            List.of(2, 6, 1), List.of(3, 4, 2), List.of(3, 5, 6),
            List.of(4, 5, 1), List.of(6, 7, 1));

    /** Rush factors ×10, parallel to TRAFFIC_EDGES (§B2). */
    private static final List<Integer> RUSH_TENTHS = List.of(
            15, 15, 10, 30, 50, 10, 10, 10, 10, 10, 10);

    /** The TSP demo points (Phase 23 design §B3). */
    private static final String TSP_POINTS =
            "10,50;20,20;20,80;80,20;80,80;50,50";

    /** The controller under test: nulls for every service except
     * the Phase 23 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 44
     * Phase 1–22 params, then quadtree, kdtree, rtree, yen,
     * traffic, tsp (50 total). */
    

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    /** A request with word + values + capacity set. */
    private TraceRequest spatialRequest(String word,
                                        List<Integer> values,
                                        Integer capacity) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        request.setValues(values);
        request.setCapacity(capacity);
        return request;
    }

    /** A request with graphEdges + start + target + index set. */
    private TraceRequest graphRequest(List<List<Integer>> edges,
                                      Integer start, Integer target,
                                      Integer index) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setStart(start);
        request.setTarget(target);
        request.setIndex(index);
        return request;
    }

    private List<Integer> lines(List<Step> steps) {
        return steps.stream().map(Step::getLine).toList();
    }

    // ---- Quadtree tests ----

    @Test
    void quadtreeDemo() {
        List<Step> steps = quadtreeService.quadtree(
                QUAD_POINTS, List.of(50, 50, 100, 100), 2);
        assertEquals(16, steps.size());
        assertEquals(List.of(2, 2, 3, 2, 2, 3,
                4, 5, 4, 4, 4, 4, 4, 5, 5, 0), lines(steps));
        assertTrue(steps.get(2).getMessage().contains(
                "NW=[B], NE=[], SW=[A], SE=[C]"),
                steps.get(2).getMessage());
        assertTrue(steps.get(5).getMessage().contains(
                "NW=[], NE=[D], SW=[E, F], SE=[]"),
                steps.get(5).getMessage());
        assertEquals(List.of(55, 55, 60, 60, 80, 80),
                steps.get(15).getData());
        assertEquals("Query (50,50,100,100) found 3 points: "
                + "(55,55), (60,60), (80,80).",
                steps.get(15).getMessage());
        // The split root's children carry the split points.
        assertEquals(List.of(10, 10),
                steps.get(2).getNodes().get(3).getKeys());
        assertEquals("0,0,50,50",
                steps.get(2).getNodes().get(3).getLabel());
    }

    @Test
    void quadtreeSwQuery() {
        List<Step> steps = quadtreeService.quadtree(
                QUAD_POINTS, List.of(0, 0, 50, 50), 2);
        assertEquals(12, steps.size());
        assertEquals(List.of(10, 10), steps.get(11).getData());
        assertEquals("Query (0,0,50,50) found 1 point: (10,10).",
                steps.get(11).getMessage());
    }

    @Test
    void quadtreeCapacityOne() {
        List<Step> steps = quadtreeService.quadtree(
                "10,10;20,80", List.of(0, 0, 100, 100), 1);
        // 2 inserts (store, split) + 5 query visits + final.
        assertEquals(8, steps.size());
        assertEquals(List.of(2, 3, 4, 4, 4, 4, 4, 0), lines(steps));
        assertEquals(List.of(20, 80), steps.get(1).getData());
        assertEquals(List.of(10, 10, 20, 80), steps.get(7).getData());
    }

    // ---- K-d tree tests ----

    @Test
    void kdTreeDemo() {
        List<Step> steps = kdTreeService.kdtree(KD_POINTS, List.of(9, 2));
        assertEquals(6, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 3, 0), lines(steps));
        assertEquals(List.of(5, 4, 4472), steps.get(0).getData());
        assertEquals(List.of(7, 2, 2000), steps.get(1).getData());
        assertEquals(List.of(7, 2, 2000), steps.get(2).getData());
        assertEquals(List.of(8, 1, 1414), steps.get(3).getData());
        assertEquals(List.of(8, 1, 1414), steps.get(4).getData());
        assertEquals(List.of(8, 1, 1414), steps.get(5).getData());
        assertTrue(steps.get(4).getMessage().contains("Prune at (5,4)"),
                steps.get(4).getMessage());
        assertEquals("Nearest to (9,2) is (8,1), distance 1.414.",
                steps.get(5).getMessage());
        // The built tree: root (5,4) axis x, region (2,1,9,7).
        assertEquals(List.of(2, 1, 9, 7),
                steps.get(0).getNodes().get(0).getKeys());
        assertEquals(0, steps.get(0).getNodes().get(0).getValue());
        // (7,2) axis y, region (5,1,9,7); (8,1) axis x, region (5,1,9,2).
        assertEquals(List.of(5, 1, 9, 7),
                steps.get(0).getNodes().get(2).getKeys());
        assertEquals(List.of(5, 1, 9, 2),
                steps.get(0).getNodes().get(4).getKeys());
    }

    @Test
    void kdTreeOtherQuery() {
        List<Step> steps = kdTreeService.kdtree(KD_POINTS, List.of(6, 3));
        assertEquals(10, steps.size());
        assertEquals(List.of(5, 4, 1414), steps.get(9).getData());
        assertEquals("Nearest to (6,3) is (5,4), distance 1.414.",
                steps.get(9).getMessage());
    }

    // ---- R-tree tests ----

    @Test
    void rtreeDemo() {
        List<Step> steps = rTreeService.rtree(
                RTREE_RECTS, List.of(12, 12, 30, 30), 4);
        assertEquals(13, steps.size());
        assertEquals(List.of(3, 3, 3, 3, 3, 4, 5, 2, 3, 6, 6, 7, 0),
                lines(steps));
        assertTrue(steps.get(5).getMessage().contains(
                "[R4, R3] / [R1, R2, R5]"), steps.get(5).getMessage());
        assertTrue(steps.get(6).getMessage().contains(
                "New root (10,10,75,70)"), steps.get(6).getMessage());
        assertTrue(steps.get(7).getMessage().contains(
                "Descend into (60,10,75,25)"), steps.get(7).getMessage());
        assertEquals(List.of(0, 1), steps.get(12).getData());
        assertEquals("Query (12,12,30,30) found 2 rectangles: R1, R2.",
                steps.get(12).getMessage());
        // The final tree: root (10,10,75,70), leaf (60,10,75,70)
        // holding R4, R3, R6 and leaf (10,10,25,70) holding R1, R2, R5.
        assertEquals("10,10,75,70",
                steps.get(12).getNodes().get(0).getLabel());
        assertEquals(List.of(3, 2, 5),
                steps.get(12).getNodes().get(1).getKeys());
        assertEquals(List.of(0, 1, 4),
                steps.get(12).getNodes().get(2).getKeys());
    }

    @Test
    void rtreeOtherQueries() {
        List<Step> south = rTreeService.rtree(
                RTREE_RECTS, List.of(55, 55, 80, 80), 4);
        assertEquals(13, south.size());
        assertEquals(List.of(5), south.get(12).getData());
        assertEquals("Query (55,55,80,80) found 1 rectangle: R6.",
                south.get(12).getMessage());
        List<Step> all = rTreeService.rtree(
                RTREE_RECTS, List.of(0, 0, 100, 100), 4);
        assertEquals(15, all.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), all.get(14).getData());
        assertEquals("Query (0,0,100,100) found 6 rectangles: "
                + "R1, R2, R3, R4, R5, R6.", all.get(14).getMessage());
    }

    // ---- Yen tests ----

    @Test
    void yenDemo() {
        List<Step> steps = yenService.yen(YEN_EDGES, 0, 5, 3);
        assertEquals(22, steps.size());
        assertEquals(List.of(2, 3, 4, 3, 4, 3, 5, 3, 4, 3, 5, 2,
                3, 5, 3, 4, 3, 4, 3, 5, 2, 0), lines(steps));
        assertEquals(List.of(1), steps.get(0).getData());
        assertEquals("Path 1 (shortest): 0 → 2 → 1 → 3 → 4 → 5, "
                + "length 13.", steps.get(0).getMessage());
        assertEquals(List.of(2, 14), steps.get(2).getData());
        assertEquals(List.of(2, 15), steps.get(4).getData());
        assertEquals(List.of(2, 14), steps.get(8).getData());
        assertEquals("Path 2: 0 → 1 → 3 → 4 → 5, length 14.",
                steps.get(11).getMessage());
        assertEquals(List.of(3, 18), steps.get(15).getData());
        assertEquals(List.of(3, 15), steps.get(17).getData());
        assertEquals("Path 3: 0 → 2 → 1 → 3 → 5, length 14.",
                steps.get(20).getMessage());
        assertEquals(List.of(13, 14, 14), steps.get(21).getData());
        assertTrue(steps.get(21).getMessage().contains(
                "Path 1: 0 → 2 → 1 → 3 → 4 → 5 (13)."),
                steps.get(21).getMessage());
        // sorted carries the path the visualizer draws: the chosen
        // path on chosen steps, the root prefix on spur/no-spur
        // steps, the candidate on candidate steps, the last path
        // on the final step (integration catch 2026-11-20).
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                steps.get(0).getSorted());
        assertEquals(List.of(0), steps.get(1).getSorted());
        assertEquals(List.of(0, 1, 3, 4, 5), steps.get(2).getSorted());
        assertEquals(List.of(0, 2, 1), steps.get(5).getSorted());
        assertEquals(List.of(0, 2, 1), steps.get(6).getSorted());
        assertEquals(List.of(0, 1, 3, 4, 5), steps.get(11).getSorted());
        assertEquals(List.of(0, 2, 1, 3, 5), steps.get(20).getSorted());
        assertEquals(List.of(0, 2, 1, 3, 5), steps.get(21).getSorted());
        // During a spur search the banned edge is excluded: the
        // k2 spur-0 candidate step has no 0-2 edge.
        assertTrue(steps.get(2).getEdges().stream().noneMatch(e ->
                (e.getFrom() == 0 && e.getTo() == 2)
                        || (e.getFrom() == 2 && e.getTo() == 0)));
    }

    @Test
    void yenK1AndK2() {
        List<Step> one = yenService.yen(YEN_EDGES, 0, 5, 1);
        assertEquals(2, one.size());
        assertEquals(List.of(13), one.get(1).getData());
        List<Step> two = yenService.yen(YEN_EDGES, 0, 5, 2);
        assertEquals(13, two.size());
        assertEquals(List.of(13, 14), two.get(12).getData());
    }

    // ---- Traffic tests ----

    @Test
    void trafficDemo8am() {
        List<Step> steps = trafficService.traffic(
                TRAFFIC_EDGES, 0, 5, 8, RUSH_TENTHS);
        assertEquals(18, steps.size());
        assertEquals(List.of(8, 30), steps.get(0).getData());
        assertEquals("Relax edge 0 -> 1: effective 3 "
                + "(base 2 × 1.5, rush hour).", steps.get(0).getMessage());
        assertEquals(List.of(90), steps.get(17).getData());
        assertEquals("Shortest path: 0 → 1 → 2 → 4 → 5, length 9. "
                + "Departed 8:00 (rush hour).", steps.get(17).getMessage());
        // Effective weights ×10 on the edges, in input order.
        List<Integer> weights = steps.get(0).getEdges().stream()
                .map(e -> e.getWeight()).toList();
        assertEquals(List.of(30, 60, 10, 150, 50, 40, 10, 20, 60, 10, 10),
                weights);
        // Settle order: 0, 1, 2, 6, 7, 4, 3, 5.
        List<String> settles = steps.stream()
                .filter(s -> s.getLine() == 2)
                .map(Step::getMessage).toList();
        assertEquals(8, settles.size());
        assertTrue(settles.get(0).contains("Settle node 0"),
                settles.get(0));
        assertTrue(settles.get(7).contains("Settle node 5"),
                settles.get(7));
        // The relax step highlights both endpoints (the
        // visualizer draws the relaxed edge active only then);
        // the final step's sorted is the route, not the settle
        // order (integration catch 2026-11-20); settle steps
        // keep sorted = the settle order (Dijkstra precedent).
        assertEquals(List.of(0, 1), steps.get(0).getHighlight());
        assertEquals(List.of(0, 1, 2, 4, 5), steps.get(17).getSorted());
        List<Step> settleSteps = steps.stream()
                .filter(s -> s.getLine() == 2).toList();
        assertEquals(List.of(0, 1, 2, 6, 7, 4, 3, 5),
                settleSteps.get(7).getSorted());
    }

    @Test
    void trafficDemo10am() {
        List<Step> steps = trafficService.traffic(
                TRAFFIC_EDGES, 0, 5, 10, RUSH_TENTHS);
        assertEquals(20, steps.size());
        assertEquals(List.of(10, 20), steps.get(0).getData());
        assertEquals("Relax edge 0 -> 1: effective 2 "
                + "(base 2, off-peak).", steps.get(0).getMessage());
        assertEquals(List.of(70), steps.get(19).getData());
        assertEquals("Shortest path: 0 → 1 → 2 → 3 → 4 → 5, length 7. "
                + "Departed 10:00 (off-peak).", steps.get(19).getMessage());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(19).getSorted());
    }

    // ---- TSP tests ----

    @Test
    void tspDemo() {
        List<Step> steps = tspService.tsp(TSP_POINTS);
        assertEquals(9, steps.size());
        assertEquals(List.of(2, 2, 2, 2, 2, 3, 3, 4, 0), lines(steps));
        assertEquals(List.of(632), steps.get(0).getData());
        assertEquals(List.of(1140), steps.get(1).getData());
        assertEquals(List.of(1481), steps.get(2).getData());
        assertEquals(List.of(2526), steps.get(3).getData());
        assertEquals(List.of(3126), steps.get(4).getData());
        assertEquals(List.of(2940), steps.get(5).getData());
        assertEquals(List.of(2681), steps.get(6).getData());
        assertEquals(List.of(2681), steps.get(7).getData());
        assertEquals(List.of(2681), steps.get(8).getData());
        assertTrue(steps.get(5).getMessage().contains("(i=2, j=5)"),
                steps.get(5).getMessage());
        assertTrue(steps.get(6).getMessage().contains("(i=4, j=5)"),
                steps.get(6).getMessage());
        assertEquals("Tour: A → B → D → E → F → C, length 268.1.",
                steps.get(8).getMessage());
        // The final tour edges: A-B, B-D, D-E, E-F, F-C, C-A.
        List<String> edges = steps.get(8).getEdges().stream()
                .map(e -> e.getFrom() + "-" + e.getTo()).toList();
        assertEquals(List.of("0-1", "1-3", "3-4", "4-5", "5-2", "2-0"),
                edges);
    }

    // ---- 400 tests (controller validation) ----



    /** A traffic request with graphEdges + start + target + hour + values. */
    private TraceRequest trafficRequest(List<List<Integer>> edges,
                                        Integer start, Integer target,
                                        Integer hour,
                                        List<Integer> values) {
        TraceRequest request = graphRequest(edges, start, target, hour);
        request.setValues(values);
        return request;
    }

    
}
