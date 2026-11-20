package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.BidirectionalDijkstraService;
import com.sarat.dsavisualizer.service.ContractionHierarchiesService;
import com.sarat.dsavisualizer.service.GeohashService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Phase 22 service tests. Bidirectional Dijkstra tests check
 * the demo (street network 0-1-2-3-4-5 plus side street 2-6-7:
 * shortest 7, path [0,1,2,3,4,5], meeting edge (2,3), settles
 * 5 vs plain Dijkstra 8, mu 13 → 11 → 9 → 8 → 7, 22 steps)
 * plus a no-path case and a single-edge case. Contraction
 * Hierarchies tests check the demo (the standard Dijkstra
 * graph, order [2,1,4,0,3,5]: shortcuts [(0,1,3 via 2),
 * (0,3,8 via 1), (3,5,5 via 4)], ranks {2:0, 1:1, 4:2, 0:3,
 * 3:4, 5:5}, query dist 13, unpacked path [0,2,1,3,4,5],
 * meeting node 5, settles 3 vs plain 6, 16 steps) plus the
 * default (ascending) order on the same graph — the query
 * still finds length 13 via the unpacked path [0,2,1,3,4,5]
 * (the backward upward frontier empties first there, so the
 * forward search keeps going until its own min-key >= mu;
 * see the service Javadoc). Geohash tests check the demo
 * (Prosper, TX 33.236,-96.786, precision 6: hash "9vgjjd",
 * 30 bits, the 8 neighbours, 31 steps), the Wikipedia
 * example (57.64911, 10.40744 → "u4pruydqqvj" at precision
 * 11) and precision 1. The 400 tests call the controller
 * endpoints directly — the controller is built with nulls for
 * the other services because the validators 400 before any
 * service is touched (the Phase 16 precedent).
 */
class Phase22ServiceTest {

    private final BidirectionalDijkstraService bidijkstraService =
            new BidirectionalDijkstraService();
    private final ContractionHierarchiesService chService =
            new ContractionHierarchiesService();
    private final GeohashService geohashService = new GeohashService();

    /** The demo street network (Phase 22 design §A1). */
    private static final List<List<Integer>> BID_EDGES = List.of(
            List.of(0, 1, 2), List.of(0, 2, 4), List.of(1, 2, 1),
            List.of(1, 3, 5), List.of(2, 3, 1), List.of(2, 4, 4),
            List.of(2, 6, 1), List.of(3, 4, 2), List.of(3, 5, 6),
            List.of(4, 5, 1), List.of(6, 7, 1));

    /** The demo graph for CH (Phase 22 design §A2). */
    private static final List<List<Integer>> CH_EDGES = List.of(
            List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, 1),
            List.of(1, 3, 5), List.of(2, 3, 8), List.of(2, 4, 10),
            List.of(3, 4, 2), List.of(3, 5, 6), List.of(4, 5, 3));

    /** The demo contraction order (Phase 22 design §A2). */
    private static final List<Integer> CH_ORDER =
            List.of(2, 1, 4, 0, 3, 5);

    /** The demo point (Phase 22 design §A3). */
    private static final String DEMO_LATLON = "33.236,-96.786";

    /** The demo geohash bits (30, precision 6). */
    private static final List<Integer> DEMO_BITS = List.of(
            0, 1, 0, 0, 1, 1, 1, 0, 1, 1,
            0, 1, 1, 1, 1, 1, 0, 0, 0, 1,
            1, 0, 0, 0, 1, 0, 1, 1, 0, 0);

    /** The demo final message (Phase 22 design §A3). */
    private static final String DEMO_GEOHASH_MESSAGE =
            "Geohash (precision 6): '9vgjjd'. Neighbours: N 9vgjje, "
                    + "S 9vgjj9, E 9vgjjf, W 9vgjj6, NE 9vgjjg, "
                    + "NW 9vgjj7, SE 9vgjjc, SW 9vgjj3.";

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** A request with graphEdges + start + target set. */
    private TraceRequest graphRequest(List<List<Integer>> edges,
                                      Integer start, Integer target) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setStart(start);
        request.setTarget(target);
        return request;
    }

    /** A request with graphEdges + start + target + values set. */
    private TraceRequest chRequest(List<List<Integer>> edges,
                                   Integer start, Integer target,
                                   List<Integer> order) {
        TraceRequest request = graphRequest(edges, start, target);
        request.setValues(order);
        return request;
    }

    /** A request with word + index set. */
    private TraceRequest geohashRequest(String word, Integer precision) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        request.setIndex(precision);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the Phase 22 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 41
     * Phase 1–21 params, then bidijkstra, ch, geohash
     * (44 total). */
    private VisualizationController controller() {
        return new VisualizationController(
                null, null, null, null, null, null, null,
                null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                null, null,
                null, null, null,
                null, null, null,
                bidijkstraService, chService, geohashService);
    }

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- Bidirectional Dijkstra tests ----

    @Test
    void bidijkstraDemo() {
        List<Step> steps = bidijkstraService.bidijkstra(BID_EDGES, 0, 5);
        // 5 settles + 11 relaxes + 5 mu + final = 22 steps.
        assertEquals(22, steps.size());
        assertEquals("Forward: settle node 0 (dist 0).",
                steps.get(0).getMessage());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(0, -1, -1, -1, -1, -1, -1, -1),
                steps.get(0).getData());
        assertEquals(List.of(-1, -1, -1, -1, -1, 0, -1, -1),
                steps.get(0).getAux());
        assertEquals("Relax 0 -> 1: 2.", steps.get(1).getMessage());
        assertEquals(3, steps.get(1).getLine());
        assertEquals(List.of(0, 1), steps.get(1).getHighlight());
        assertEquals(List.of(0, 2, -1, -1, -1, -1, -1, -1),
                steps.get(1).getData());
        assertEquals("Relax 0 -> 2: 4.", steps.get(2).getMessage());
        assertEquals(List.of(0, 2, 4, -1, -1, -1, -1, -1),
                steps.get(2).getData());
        assertEquals("Backward: settle node 5 (dist 0).",
                steps.get(3).getMessage());
        assertEquals(6, steps.get(3).getLine());
        assertEquals(List.of(5), steps.get(3).getHighlight());
        assertEquals("Relax 5 -> 3: 6.", steps.get(4).getMessage());
        assertEquals(7, steps.get(4).getLine());
        assertEquals(List.of(5, 3), steps.get(4).getHighlight());
        assertEquals("Relax 5 -> 4: 1.", steps.get(5).getMessage());
        assertEquals(List.of(-1, -1, -1, 6, 1, 0, -1, -1),
                steps.get(5).getAux());
        assertEquals("New best path via edge 1-3: length 13.",
                steps.get(6).getMessage());
        assertEquals(8, steps.get(6).getLine());
        assertEquals(List.of(1, 3), steps.get(6).getHighlight());
        assertEquals(List.of(0, 1, 3, 5), steps.get(6).getSorted());
        assertEquals("New best path via edge 2-3: length 11.",
                steps.get(7).getMessage());
        assertEquals(List.of(0, 2, 3, 5), steps.get(7).getSorted());
        assertEquals("New best path via edge 2-4: length 9.",
                steps.get(8).getMessage());
        assertEquals(List.of(0, 2, 4, 5), steps.get(8).getSorted());
        assertEquals("Forward: settle node 1 (dist 2).",
                steps.get(9).getMessage());
        assertEquals(List.of(0, 2, 3, -1, -1, -1, -1, -1),
                steps.get(10).getData());
        assertEquals("Relax 1 -> 3: 7.", steps.get(11).getMessage());
        assertEquals(List.of(0, 2, 3, 7, -1, -1, -1, -1),
                steps.get(11).getData());
        assertEquals("New best path via edge 2-4: length 8.",
                steps.get(12).getMessage());
        assertEquals(4, steps.get(12).getLine());
        assertEquals(List.of(2, 4), steps.get(12).getHighlight());
        assertEquals(List.of(0, 1, 2, 4, 5), steps.get(12).getSorted());
        assertEquals("Backward: settle node 4 (dist 1).",
                steps.get(13).getMessage());
        assertEquals("Relax 4 -> 2: 5.", steps.get(14).getMessage());
        assertEquals("Relax 4 -> 3: 3.", steps.get(15).getMessage());
        assertEquals(List.of(-1, -1, 5, 3, 1, 0, -1, -1),
                steps.get(15).getAux());
        assertEquals("New best path via edge 2-3: length 7.",
                steps.get(16).getMessage());
        assertEquals(8, steps.get(16).getLine());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), steps.get(16).getSorted());
        assertEquals("Forward: settle node 2 (dist 3).",
                steps.get(17).getMessage());
        assertEquals("Relax 2 -> 3: 4.", steps.get(18).getMessage());
        assertEquals(List.of(0, 2, 3, 4, -1, -1, -1, -1),
                steps.get(18).getData());
        assertEquals("Relax 2 -> 4: 7.", steps.get(19).getMessage());
        assertEquals("Relax 2 -> 6: 4.", steps.get(20).getMessage());
        assertEquals(List.of(0, 2, 3, 4, 7, -1, 4, -1),
                steps.get(20).getData());
        // Final step.
        assertEquals("Shortest path: 0 \u2192 1 \u2192 2 \u2192 3 \u2192 4 "
                        + "\u2192 5, length 7. Bidirectional settled "
                        + "5 nodes.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                lastStep(steps).getSorted());
        assertEquals(List.of(0, 2, 3, 4, 7, -1, 4, -1),
                lastStep(steps).getData());
        assertEquals(List.of(-1, -1, 5, 3, 1, 0, -1, -1),
                lastStep(steps).getAux());
        assertEquals(8, lastStep(steps).getNodes().size());
        assertEquals(11, lastStep(steps).getEdges().size());
        int settleSteps = 0;
        for (Step step : steps) {
            if (step.getLine() == 2 || step.getLine() == 6) {
                settleSteps++;
            }
        }
        assertEquals(5, settleSteps);
    }

    @Test
    void bidijkstraNoPath() {
        List<Step> steps = bidijkstraService.bidijkstra(List.of(
                List.of(0, 1, 1), List.of(2, 3, 1)), 0, 3);
        // F settle 0, F relax 1, B settle 3, B relax 2,
        // F settle 1, final = 6 steps.
        assertEquals(6, steps.size());
        assertEquals("No path from 0 to 3.", lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(), lastStep(steps).getSorted());
    }

    @Test
    void bidijkstraSingleEdge() {
        List<Step> steps = bidijkstraService.bidijkstra(
                List.of(List.of(0, 1, 5)), 0, 1);
        // F settle 0, F relax 1, mu, final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("New best path via edge 0-1: length 5.",
                steps.get(2).getMessage());
        assertEquals(List.of(0, 1), steps.get(2).getSorted());
        assertEquals("Shortest path: 0 \u2192 1, length 5. "
                        + "Bidirectional settled 1 nodes.",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 1), lastStep(steps).getSorted());
    }

    // ---- Contraction Hierarchies tests ----

    @Test
    void chDemo() {
        List<Step> steps = chService.contractionHierarchies(
                CH_EDGES, 0, 5, CH_ORDER);
        // 6 contracts + 3 shortcuts + 6 query steps + final = 16.
        assertEquals(16, steps.size());
        assertEquals("Contract node 2 (rank 0): 4 neighbours, "
                        + "1 shortcut added.",
                steps.get(0).getMessage());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(List.of(2), steps.get(0).getHighlight());
        assertEquals(List.of(-1, -1, 0, -1, -1, -1),
                steps.get(0).getData());
        assertEquals(9, steps.get(0).getEdges().size());
        assertEquals("Shortcut 0\u20131 (weight 3) via node 2; best "
                        + "alternative without 2 is 4.",
                steps.get(1).getMessage());
        assertEquals(4, steps.get(1).getLine());
        assertEquals(List.of(0, 1), steps.get(1).getHighlight());
        assertEquals(9, steps.get(1).getEdges().size());
        assertEquals(3, steps.get(1).getEdges().get(0).getWeight());
        assertEquals("Contract node 1 (rank 1): 2 neighbours, "
                        + "1 shortcut added.",
                steps.get(2).getMessage());
        assertEquals(List.of(-1, 1, 0, -1, -1, -1),
                steps.get(2).getData());
        assertEquals("Shortcut 0\u20133 (weight 8) via node 1; no "
                        + "alternative without 1.",
                steps.get(3).getMessage());
        assertEquals(10, steps.get(3).getEdges().size());
        assertEquals("Contract node 4 (rank 2): 2 neighbours, "
                        + "1 shortcut added.",
                steps.get(4).getMessage());
        assertEquals(List.of(-1, 1, 0, -1, 2, -1),
                steps.get(4).getData());
        assertEquals("Shortcut 3\u20135 (weight 5) via node 4; best "
                        + "alternative without 4 is 6.",
                steps.get(5).getMessage());
        assertEquals(10, steps.get(5).getEdges().size());
        assertEquals("Contract node 0 (rank 3): 1 neighbour, "
                        + "0 shortcuts added.",
                steps.get(6).getMessage());
        assertEquals(List.of(3, 1, 0, -1, 2, -1),
                steps.get(6).getData());
        assertEquals("Contract node 3 (rank 4): 1 neighbour, "
                        + "0 shortcuts added.",
                steps.get(7).getMessage());
        assertEquals(List.of(3, 1, 0, 4, 2, -1),
                steps.get(7).getData());
        assertEquals("Contract node 5 (rank 5): 0 neighbours, "
                        + "0 shortcuts added.",
                steps.get(8).getMessage());
        assertEquals(List.of(3, 1, 0, 4, 2, 5),
                steps.get(8).getData());
        // Query steps.
        assertEquals("Forward (upward): settle node 0 (dist 0).",
                steps.get(9).getMessage());
        assertEquals(6, steps.get(9).getLine());
        assertEquals(List.of(0), steps.get(9).getHighlight());
        assertEquals(List.of(0, -1, -1, -1, -1, -1),
                steps.get(9).getData());
        assertEquals(List.of(-1, -1, -1, -1, -1, 0),
                steps.get(9).getAux());
        assertEquals("Relax 0 -> 3: 8.", steps.get(10).getMessage());
        assertEquals(7, steps.get(10).getLine());
        assertEquals(List.of(0, 3), steps.get(10).getHighlight());
        assertEquals(List.of(0, -1, -1, 8, -1, -1),
                steps.get(10).getData());
        assertEquals("Backward (upward): settle node 5 (dist 0).",
                steps.get(11).getMessage());
        assertEquals(8, steps.get(11).getLine());
        assertEquals(List.of(5), steps.get(11).getHighlight());
        assertEquals("Forward (upward): settle node 3 (dist 8).",
                steps.get(12).getMessage());
        assertEquals("Relax 3 -> 5: 13.", steps.get(13).getMessage());
        assertEquals(List.of(0, -1, -1, 8, -1, 13),
                steps.get(13).getData());
        assertEquals("Meeting node 5: 13 + 0 = 13.",
                steps.get(14).getMessage());
        assertEquals(10, steps.get(14).getLine());
        assertEquals(List.of(5), steps.get(14).getHighlight());
        assertEquals(List.of(5), steps.get(14).getSorted());
        // Final step: the unpacked path, 10 edges (9 original
        // pairs + the new (0,3) shortcut; the (0,1) and (3,5)
        // shortcuts overwrite their original pairs' weights).
        assertEquals("Shortest path: 0 \u2192 2 \u2192 1 \u2192 3 \u2192 4 "
                        + "\u2192 5, length 13 (unpacked from shortcuts). "
                        + "CH settled 3 nodes.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                lastStep(steps).getSorted());
        assertEquals(List.of(0, -1, -1, 8, -1, 13),
                lastStep(steps).getData());
        assertEquals(List.of(-1, -1, -1, -1, -1, 0),
                lastStep(steps).getAux());
        assertEquals(6, lastStep(steps).getNodes().size());
        assertEquals(10, lastStep(steps).getEdges().size());
        assertEquals(3, lastStep(steps).getEdges().get(0).getWeight());
        assertEquals(5, lastStep(steps).getEdges().get(7).getWeight());
        assertEquals(8, lastStep(steps).getEdges().get(9).getWeight());
    }

    @Test
    void chDefaultOrder() {
        // No order given: ascending node id [0,1,2,3,4,5].
        List<Step> steps = chService.contractionHierarchies(
                CH_EDGES, 0, 5, null);
        // 6 contracts + 1 shortcut + 15 query steps + final = 23.
        assertEquals(23, steps.size());
        assertEquals("Contract node 0 (rank 0): 2 neighbours, "
                        + "0 shortcuts added.",
                steps.get(0).getMessage());
        assertEquals(List.of(0, -1, -1, -1, -1, -1),
                steps.get(0).getData());
        assertEquals("Contract node 1 (rank 1): 2 neighbours, "
                        + "1 shortcut added.",
                steps.get(1).getMessage());
        assertEquals("Shortcut 2\u20133 (weight 6) via node 1; best "
                        + "alternative without 1 is 8.",
                steps.get(2).getMessage());
        assertEquals(4, steps.get(2).getLine());
        assertEquals(List.of(0, 1, -1, -1, -1, -1),
                steps.get(2).getData());
        assertEquals("Contract node 2 (rank 2): 2 neighbours, "
                        + "0 shortcuts added.",
                steps.get(3).getMessage());
        assertEquals("Contract node 3 (rank 3): 2 neighbours, "
                        + "0 shortcuts added.",
                steps.get(4).getMessage());
        assertEquals("Contract node 4 (rank 4): 1 neighbour, "
                        + "0 shortcuts added.",
                steps.get(5).getMessage());
        assertEquals("Contract node 5 (rank 5): 0 neighbours, "
                        + "0 shortcuts added.",
                steps.get(6).getMessage());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(6).getData());
        // The query still finds length 13 (unpacked path
        // [0,2,1,3,4,5]); settles 6 (forward 5 + backward 1).
        assertEquals("Meeting node 5: 14 + 0 = 14.",
                steps.get(18).getMessage());
        assertEquals("Forward (upward): settle node 4 (dist 10).",
                steps.get(19).getMessage());
        assertEquals("Relax 4 -> 5: 13.", steps.get(20).getMessage());
        assertEquals("Meeting node 5: 13 + 0 = 13.",
                steps.get(21).getMessage());
        assertEquals("Shortest path: 0 \u2192 2 \u2192 1 \u2192 3 \u2192 4 "
                        + "\u2192 5, length 13 (unpacked from shortcuts). "
                        + "CH settled 6 nodes.",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                lastStep(steps).getSorted());
    }

    @Test
    void chNoPath() {
        List<Step> steps = chService.contractionHierarchies(List.of(
                List.of(0, 1, 1), List.of(2, 3, 1)), 0, 3,
                List.of(0, 1, 2, 3));
        assertEquals("No path from 0 to 3.", lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(), lastStep(steps).getSorted());
    }

    // ---- Geohash tests ----

    @Test
    void geohashDemo() {
        List<Step> steps = geohashService.geohash(DEMO_LATLON, 6);
        // 30 bit steps + final = 31.
        assertEquals(31, steps.size());
        assertEquals("Bit 1 (lon): -96.786 in lower half [-180, 0] "
                        + "\u2192 0.",
                steps.get(0).getMessage());
        assertEquals(3, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(1), steps.get(0).getAux());
        assertEquals("Bit 2 (lat): 33.236 in upper half [0, 90] "
                        + "\u2192 1.",
                steps.get(1).getMessage());
        assertEquals(4, steps.get(1).getLine());
        assertEquals(List.of(0, 1), steps.get(1).getData());
        assertEquals(List.of(1), steps.get(1).getHighlight());
        assertEquals(List.of(2), steps.get(1).getAux());
        assertEquals("Bit 5 (lon): -96.786 in upper half [-135, -90] "
                        + "\u2192 1; char '9'.",
                steps.get(4).getMessage());
        assertEquals(List.of(0, 1, 0, 0, 1), steps.get(4).getData());
        assertEquals("Bit 10 (lat): 33.236 in upper half "
                        + "[28.125, 33.75] \u2192 1; char 'v'.",
                steps.get(9).getMessage());
        assertEquals("Bit 30 (lat): 33.236 in lower half "
                        + "[33.2336, 33.2391] \u2192 0; char 'd'.",
                steps.get(29).getMessage());
        assertEquals(DEMO_BITS, steps.get(29).getData());
        assertEquals(List.of(29), steps.get(29).getHighlight());
        assertEquals(List.of(30), steps.get(29).getAux());
        // Final step.
        assertEquals(DEMO_GEOHASH_MESSAGE, lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(DEMO_BITS, lastStep(steps).getData());
        assertEquals(List.of(6), lastStep(steps).getSorted());
        assertEquals(List.of(30), lastStep(steps).getAux());
        // A bit step's line is 3 (lon) or 4 (lat), alternating.
        for (int i = 0; i < 30; i++) {
            assertEquals(i % 2 == 0 ? 3 : 4, steps.get(i).getLine());
            assertEquals(List.of(i + 1), steps.get(i).getAux());
        }
    }

    @Test
    void geohashWikipediaExample() {
        List<Step> steps = geohashService.geohash("57.64911,10.40744", 11);
        // 55 bit steps + final = 56.
        assertEquals(56, steps.size());
        assertEquals(55, lastStep(steps).getData().size());
        assertEquals(List.of(11), lastStep(steps).getSorted());
        assertEquals("Geohash (precision 11): 'u4pruydqqvj'. "
                        + "Neighbours: N u4pruydqqvm, S u4pruydqquv, "
                        + "E u4pruydqqvn, W u4pruydqqvh, NE u4pruydqqvq, "
                        + "NW u4pruydqqvk, SE u4pruydqquy, SW u4pruydqquu.",
                lastStep(steps).getMessage());
    }

    @Test
    void geohashPrecisionOne() {
        List<Step> steps = geohashService.geohash(DEMO_LATLON, 1);
        // 5 bit steps + final = 6.
        assertEquals(6, steps.size());
        assertEquals("Bit 5 (lon): -96.786 in upper half [-135, -90] "
                        + "\u2192 1; char '9'.",
                steps.get(4).getMessage());
        assertEquals("Geohash (precision 1): '9'. Neighbours: N c, "
                        + "S 3, E d, W 8, NE f, NW b, SE 6, SW 2.",
                lastStep(steps).getMessage());
        assertEquals(List.of(1), lastStep(steps).getSorted());
    }

    @Test
    void geohashDefaultPrecision() {
        // A missing precision defaults to 6: same hash as the demo.
        List<Step> steps = geohashService.geohash(DEMO_LATLON, null);
        assertEquals(31, steps.size());
        assertEquals(DEMO_GEOHASH_MESSAGE, lastStep(steps).getMessage());
    }

    // ---- 400 tests (controller validation) ----

    @Test
    void bidijkstra400() {
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(null, 0, 5)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(List.of(), 0, 5)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(List.of(
                        List.of(0, 1, 0), List.of(1, 2, 1)), 0, 2)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(List.of(
                        List.of(0, 1, -3), List.of(1, 2, 1)), 0, 2)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(BID_EDGES, null, 5)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(BID_EDGES, 0, null)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(BID_EDGES, 0, 0)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(BID_EDGES, 9, 5)));
        assertBadRequest(controller().graphBidijkstra(
                graphRequest(BID_EDGES, 0, 9)));
        // A valid request returns 200 (a null weight defaults to 1).
        assertEquals(200, controller().graphBidijkstra(
                graphRequest(BID_EDGES, 0, 5))
                .getStatusCode().value());
        assertEquals(200, controller().graphBidijkstra(
                graphRequest(List.of(
                        List.of(0, 1), List.of(1, 2, 3)), 0, 2))
                .getStatusCode().value());
    }

    @Test
    void ch400() {
        assertBadRequest(controller().graphCh(
                chRequest(null, 0, 5, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(List.of(), 0, 5, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(List.of(
                        List.of(0, 1, 0), List.of(1, 2, 1)), 0, 2,
                        List.of(0, 1, 2))));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, null, 5, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, null, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 0, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 9, 5, CH_ORDER)));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 9, CH_ORDER)));
        // A bad order is not a permutation of the graph's nodes.
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 5, List.of(2, 1, 4, 0, 3))));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 5, List.of(2, 1, 4, 0, 3, 3))));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 5, List.of(2, 1, 4, 0, 3, 9))));
        assertBadRequest(controller().graphCh(
                chRequest(CH_EDGES, 0, 5, List.of(2, 1, 4, 0, 3, 5, 0))));
        // A valid request returns 200, with or without an order.
        assertEquals(200, controller().graphCh(
                chRequest(CH_EDGES, 0, 5, CH_ORDER))
                .getStatusCode().value());
        assertEquals(200, controller().graphCh(
                chRequest(CH_EDGES, 0, 5, null))
                .getStatusCode().value());
    }

    @Test
    void geohash400() {
        assertBadRequest(controller().geohash(
                geohashRequest(null, 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("abc", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("33.236", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("33.236,abc", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("33.236,-96.786,1", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("91,0", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("-91,0", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("0,181", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("0,-181", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest("NaN,0", 6)));
        assertBadRequest(controller().geohash(
                geohashRequest(DEMO_LATLON, 0)));
        assertBadRequest(controller().geohash(
                geohashRequest(DEMO_LATLON, 13)));
        // A valid request returns 200, with or without a precision.
        assertEquals(200, controller().geohash(
                geohashRequest(DEMO_LATLON, 6))
                .getStatusCode().value());
        assertEquals(200, controller().geohash(
                geohashRequest(DEMO_LATLON, null))
                .getStatusCode().value());
        // Whitespace around the pair is tolerated.
        assertEquals(200, controller().geohash(
                geohashRequest(" 33.236 , -96.786 ", 6))
                .getStatusCode().value());
    }
}
