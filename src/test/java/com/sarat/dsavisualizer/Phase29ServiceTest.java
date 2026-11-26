package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.Search2dService;
import com.sarat.dsavisualizer.service.SetZeroesService;
import com.sarat.dsavisualizer.service.WordSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 29 service tests. Prim's and Kruskal's tests check the
 * REAL Phase 8 services against the ground-truth traces dumped
 * from them (~/workspace/.tmp/p29-sim/mst-traces.txt): Prim's
 * on the demo graph (16 steps, 9 relax + 6 add + 1 final, MST
 * edges [[0,2],[2,1],[1,3],[3,4],[4,5]], total 13 — keys are
 * EDGE weights, and there is NO build step) and Kruskal's (13
 * steps, 6 consider+act pairs + 1 final, MST edges
 * [[1,2],[0,2],[3,4],[4,5],[1,3]], total 13 — stable sort,
 * 0-2 before 3-4, and the loop breaks after 5 edges so 3-5,
 * 2-3, 2-4 are never considered). Boruvka's tests check the
 * demo graph (18 steps, 1 build + 8 consider+act pairs + 1
 * final, same MST weight 13 — simultaneous per-phase picks:
 * component {2} picks 1-2 even though 1 and 2 are already
 * united, and it is skipped as already in the MST). Search
 * 2D tests check the demo 3x4 (target 3 found at (0,1),
 * probes (0,11,5,11), (0,4,2,5), (0,1,0,1), (1,1,1,3); 6
 * steps) and a target that is absent. Set Zeroes tests check
 * the demo 3x3 (zero at (1,1); mark writes A[0][1] and
 * A[1][0]; 5 steps), a zero in row 0, and two zeroes. Word
 * Search tests check the demo board (word "ABCCED", path
 * (0,0),(0,1),(0,2),(1,2),(2,2),(2,1), dead ends (0,3),
 * (1,3), (2,3), neighbours up/right/down/left; 11 steps) and
 * a word that is absent. The 400 tests call the controller
 * endpoints directly — the controller is built with nulls for
 * the other services because the validators 400 before any
 * service is touched (the Phase 16 precedent).
 */
class Phase29ServiceTest {

    private final GraphService graphService = new GraphService();
    private final Search2dService search2dService = new Search2dService();
    private final SetZeroesService setZeroesService = new SetZeroesService();
    private final WordSearchService wordSearchService = new WordSearchService();

    static final List<List<Integer>> DEMO_EDGES = List.of(
            List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, 1),
            List.of(1, 3, 5), List.of(2, 3, 8), List.of(2, 4, 10),
            List.of(3, 4, 2), List.of(3, 5, 6), List.of(4, 5, 3));
    static final String SEARCH_MATRIX =
            "1,3,5,7;10,11,16,20;23,30,34,60";
    static final String ZEROES_MATRIX = "1,1,1;1,0,1;1,1,1";
    static final String WORD_BOARD = "A,B,C,E;S,F,C,S;A,D,E,E";

    
    static void checkStatus(String name, int expected,
                            ResponseEntity<?> r) {
        assertEquals(expected, r.getStatusCode().value(), name);
    }

    static List<Integer> ints(int... values) {
        List<Integer> out = new ArrayList<>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }

    static List<Integer> dataWithNulls(Integer... values) {
        return Arrays.asList(values);
    }

    static List<List<Integer>> edgeList(int[][] edges) {
        List<List<Integer>> out = new ArrayList<>();
        for (int[] e : edges) {
            out.add(ints(e[0], e[1]));
        }
        return out;
    }

    // ---- Prim's (real Phase 8 service, ground truth) ----

    @Test
    void primDemoStepCount() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(16, steps.size());
        assertEquals(0, steps.get(15).getLine());
        assertEquals(dataWithNulls(0, 1, 2, 5, 2, 3),
                steps.get(15).getData());
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                steps.get(15).getSorted());
        assertEquals(edgeList(new int[][]{{0, 2}, {2, 1}, {1, 3},
                {3, 4}, {4, 5}}), steps.get(15).getMstEdges());
        assertTrue(steps.get(15).getMessage().contains("13"));
    }

    @Test
    void primDemoNoBuildStep() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(3, steps.get(0).getLine());
        assertTrue(steps.get(0).getMessage().startsWith("Relax edge 0-1"));
    }

    @Test
    void primDemoLines() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        int[] lines = {3, 3, 2, 3, 3, 3, 2, 3, 2, 3, 3, 2, 3, 2, 2, 0};
        for (int i = 0; i < 16; i++) {
            assertEquals(lines[i], steps.get(i).getLine(), "step " + i);
        }
    }

    @Test
    void primDemoKeys() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(dataWithNulls(0, 4, null, null, null, null),
                steps.get(0).getData());
        assertEquals(dataWithNulls(0, 4, 2, null, null, null),
                steps.get(1).getData());
        assertEquals(dataWithNulls(0, 1, 2, null, null, null),
                steps.get(3).getData());
        assertEquals(dataWithNulls(0, 1, 2, 8, 10, null),
                steps.get(6).getData());
        assertEquals(dataWithNulls(0, 1, 2, 5, 10, null),
                steps.get(8).getData());
        assertEquals(dataWithNulls(0, 1, 2, 5, 2, 3),
                steps.get(12).getData());
    }

    @Test
    void primDemoHighlightsAndSorted() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals(List.of(0), steps.get(2).getHighlight());
        assertEquals(List.of(2), steps.get(6).getHighlight());
        assertEquals(List.of(1), steps.get(8).getHighlight());
        assertEquals(List.of(5), steps.get(14).getHighlight());
        assertEquals(List.of(), steps.get(15).getHighlight());
        assertEquals(List.of(0), steps.get(2).getSorted());
        assertEquals(List.of(0, 2), steps.get(6).getSorted());
        assertEquals(List.of(0, 2, 1), steps.get(8).getSorted());
        assertEquals(List.of(0, 2, 1, 3), steps.get(11).getSorted());
        assertEquals(List.of(0, 2, 1, 3, 4), steps.get(13).getSorted());
        assertEquals(List.of(0, 2, 1, 3, 4, 5),
                steps.get(14).getSorted());
    }

    @Test
    void primDemoMstEdges() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(List.of(), steps.get(5).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}}),
                steps.get(6).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {2, 1}}),
                steps.get(8).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {2, 1}, {1, 3}}),
                steps.get(11).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {2, 1}, {1, 3},
                {3, 4}}), steps.get(13).getMstEdges());
    }

    @Test
    void primDemoNodesAndEdges() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertEquals(6, steps.get(0).getNodes().size());
        for (int i = 0; i < 6; i++) {
            assertEquals(i, steps.get(0).getNodes().get(i).getId());
            assertEquals(i, steps.get(0).getNodes().get(i).getValue());
            // The service sets NO label (snapshotNodes never calls
            // setLabel) — the design pin says "" but the real value
            // is null; the frontend renders the value when the
            // label is absent.
            assertNull(steps.get(0).getNodes().get(i).getLabel());
        }
        assertEquals(9, steps.get(0).getEdges().size());
        assertEquals(0, steps.get(0).getEdges().get(0).getFrom());
        assertEquals(1, steps.get(0).getEdges().get(0).getTo());
        assertEquals(4, steps.get(0).getEdges().get(0).getWeight());
        assertEquals(4, steps.get(0).getEdges().get(8).getFrom());
        assertEquals(5, steps.get(0).getEdges().get(8).getTo());
        assertEquals(3, steps.get(0).getEdges().get(8).getWeight());
    }

    @Test
    void primDemoMessages() {
        List<Step> steps = graphService.prim(DEMO_EDGES, 0);
        assertTrue(steps.get(0).getMessage()
                .contains("Relax edge 0-1: key"));
        assertTrue(steps.get(2).getMessage()
                .contains("Add node 0 to the MST. Total weight: 0."));
        assertTrue(steps.get(6).getMessage()
                .contains("Add node 2 to the MST via edge 0-2"));
        assertTrue(steps.get(14).getMessage()
                .contains("Add node 5 to the MST via edge 4-5"));
        assertTrue(steps.get(15).getMessage()
                .contains("MST total weight: 13."));
    }

    @Test
    void primEmptyAndInvalidStart() {
        List<Step> empty = graphService.prim(null, 0);
        assertEquals(1, empty.size());
        assertEquals(0, empty.get(0).getLine());
        assertTrue(empty.get(0).getMessage().contains("Graph is empty."));
        List<Step> bad = graphService.prim(DEMO_EDGES, 9);
        assertEquals(1, bad.size());
        assertTrue(bad.get(0).getMessage().contains("Invalid start node."));
    }

    // ---- Kruskal's (real Phase 8 service, ground truth) ----

    @Test
    void kruskalDemoStepCount() {
        List<Step> steps = graphService.kruskal(DEMO_EDGES);
        assertEquals(13, steps.size());
        assertEquals(0, steps.get(12).getLine());
        assertEquals(List.of(), steps.get(12).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(12).getSorted());
        assertEquals(edgeList(new int[][]{{1, 2}, {0, 2}, {3, 4},
                {4, 5}, {1, 3}}), steps.get(12).getMstEdges());
        assertTrue(steps.get(12).getMessage().contains("13"));
    }

    @Test
    void kruskalDemoLinesAndHighlights() {
        List<Step> steps = graphService.kruskal(DEMO_EDGES);
        int[][] hl = {{1, 2}, {1, 2}, {0, 2}, {0, 2}, {3, 4}, {3, 4},
                {4, 5}, {4, 5}, {0, 1}, {0, 1}, {1, 3}, {1, 3}};
        for (int i = 0; i < 12; i++) {
            assertEquals(i % 2 == 0 ? 2 : 3, steps.get(i).getLine(),
                    "step " + i);
            assertEquals(ints(hl[i][0], hl[i][1]),
                    steps.get(i).getHighlight(), "step " + i);
            assertEquals(List.of(), steps.get(i).getData(), "step " + i);
        }
        assertEquals(List.of(), steps.get(12).getHighlight());
    }

    @Test
    void kruskalDemoSorted() {
        List<Step> steps = graphService.kruskal(DEMO_EDGES);
        assertEquals(List.of(), steps.get(0).getSorted());
        assertEquals(List.of(1, 2), steps.get(1).getSorted());
        assertEquals(List.of(0, 1, 2), steps.get(3).getSorted());
        assertEquals(List.of(0, 1, 2, 3, 4), steps.get(5).getSorted());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(7).getSorted());
    }

    @Test
    void kruskalDemoSkip() {
        List<Step> steps = graphService.kruskal(DEMO_EDGES);
        assertTrue(steps.get(9).getMessage()
                .contains("Skip edge 0-1 (weight 4)"));
        assertTrue(steps.get(9).getMessage().contains("cycle"));
        assertEquals(edgeList(new int[][]{{1, 2}, {0, 2}, {3, 4},
                {4, 5}}), steps.get(9).getMstEdges());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(9).getSorted());
    }

    @Test
    void kruskalStableSortAndEarlyBreak() {
        List<Step> steps = graphService.kruskal(DEMO_EDGES);
        // Equal weights keep input order: 0-2 (2) before 3-4 (2).
        assertTrue(steps.get(2).getMessage()
                .contains("Consider edge 0-2 (weight 2)."));
        assertTrue(steps.get(4).getMessage()
                .contains("Consider edge 3-4 (weight 2)."));
        // The loop breaks after 5 MST edges: 3-5, 2-3, 2-4 are
        // never considered — no step mentions them.
        for (Step step : steps) {
            assertTrue(!step.getMessage().contains("3-5"));
            assertTrue(!step.getMessage().contains("2-3"));
            assertTrue(!step.getMessage().contains("2-4"));
        }
    }

    @Test
    void kruskalEmpty() {
        List<Step> steps = graphService.kruskal(null);
        assertEquals(1, steps.size());
        assertEquals(0, steps.get(0).getLine());
        assertTrue(steps.get(0).getMessage().contains("Graph is empty."));
    }

    // ---- Boruvka's ----

    @Test
    void boruvkaDemoStepCount() {
        List<Step> steps = graphService.boruvka(DEMO_EDGES);
        assertEquals(18, steps.size());
        assertEquals(0, steps.get(17).getLine());
        assertEquals(List.of(13, 1), steps.get(17).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(17).getSorted());
        assertEquals(edgeList(new int[][]{{0, 2}, {1, 2}, {3, 4},
                {4, 5}, {1, 3}}), steps.get(17).getMstEdges());
        assertTrue(steps.get(17).getMessage().contains("13"));
    }

    @Test
    void boruvkaDemoBuild() {
        List<Step> steps = graphService.boruvka(DEMO_EDGES);
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0, 6), steps.get(0).getData());
        assertEquals(List.of(), steps.get(0).getHighlight());
        assertEquals(List.of(), steps.get(0).getSorted());
        assertEquals(List.of(), steps.get(0).getMstEdges());
        assertTrue(steps.get(0).getMessage().contains("6 components"));
        assertEquals(6, steps.get(0).getNodes().size());
        assertEquals(9, steps.get(0).getEdges().size());
    }

    @Test
    void boruvkaDemoDataAndLines() {
        List<Step> steps = graphService.boruvka(DEMO_EDGES);
        int[][] data = {{0, 6}, {0, 6}, {2, 5}, {2, 5}, {3, 4},
                {3, 4}, {3, 4}, {3, 4}, {5, 3}, {5, 3}, {5, 3},
                {5, 3}, {8, 2}, {8, 2}, {13, 1}, {13, 1}, {13, 1},
                {13, 1}};
        int[] lines = {2, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3, 2, 3,
                2, 3, 0};
        int[][] hl = {{}, {0, 2}, {0, 2}, {1, 2}, {1, 2}, {1, 2},
                {1, 2}, {3, 4}, {3, 4}, {3, 4}, {3, 4}, {4, 5},
                {4, 5}, {1, 3}, {1, 3}, {1, 3}, {1, 3}, {}};
        for (int i = 0; i < 18; i++) {
            assertEquals(ints(data[i][0], data[i][1]),
                    steps.get(i).getData(), "step " + i);
            assertEquals(lines[i], steps.get(i).getLine(), "step " + i);
            assertEquals(ints(hl[i]), steps.get(i).getHighlight(),
                    "step " + i);
        }
    }

    @Test
    void boruvkaDemoSortedAndMstEdges() {
        List<Step> steps = graphService.boruvka(DEMO_EDGES);
        assertEquals(List.of(), steps.get(1).getSorted());
        assertEquals(List.of(0, 2), steps.get(2).getSorted());
        assertEquals(List.of(0, 1, 2), steps.get(4).getSorted());
        assertEquals(List.of(0, 1, 2, 3, 4), steps.get(8).getSorted());
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                steps.get(12).getSorted());
        assertEquals(List.of(), steps.get(1).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}}),
                steps.get(2).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {1, 2}}),
                steps.get(4).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {1, 2}, {3, 4}}),
                steps.get(8).getMstEdges());
        assertEquals(edgeList(new int[][]{{0, 2}, {1, 2}, {3, 4},
                {4, 5}}), steps.get(12).getMstEdges());
    }

    @Test
    void boruvkaSimultaneousPicks() {
        List<Step> steps = graphService.boruvka(DEMO_EDGES);
        // Component {2} picks 1-2 even though 1 and 2 are already
        // united by the earlier picks — a sequential
        // implementation would never consider it.
        assertTrue(steps.get(5).getMessage()
                .contains("Consider edge 1-2 (weight 1)"));
        assertTrue(steps.get(5).getMessage().contains("{2}"));
        assertTrue(steps.get(6).getMessage()
                .contains("Skip edge 1-2 (weight 1)"));
        assertTrue(steps.get(6).getMessage()
                .contains("already in the MST"));
        assertTrue(steps.get(10).getMessage()
                .contains("Skip edge 3-4 (weight 2)"));
        assertTrue(steps.get(13).getMessage()
                .contains("Consider edge 1-3 (weight 5)"));
        assertTrue(steps.get(13).getMessage().contains("{0, 1, 2}"));
        assertTrue(steps.get(16).getMessage()
                .contains("Skip edge 1-3 (weight 5)"));
    }

    @Test
    void boruvkaTwoNodes() {
        List<Step> steps = graphService.boruvka(
                List.of(List.of(0, 1, 7)));
        assertEquals(6, steps.size());
        assertEquals(List.of(0, 2), steps.get(0).getData());
        assertEquals(List.of(7, 1), steps.get(5).getData());
        assertEquals(edgeList(new int[][]{{0, 1}}),
                steps.get(5).getMstEdges());
        assertTrue(steps.get(5).getMessage().contains("7"));
    }

    @Test
    void boruvkaEmptyAndDisconnected() {
        List<Step> empty = graphService.boruvka(null);
        assertEquals(1, empty.size());
        assertTrue(empty.get(0).getMessage().contains("Graph is empty."));
        List<Step> disc = graphService.boruvka(List.of(
                List.of(0, 1, 1), List.of(2, 3, 1)));
        assertEquals(10, disc.size());
        assertEquals(List.of(2, 2), disc.get(9).getData());
        assertEquals(edgeList(new int[][]{{0, 1}, {2, 3}}),
                disc.get(9).getMstEdges());
        assertTrue(disc.get(9).getMessage().contains("disconnected"));
    }

    // ---- Search a 2D Matrix ----

    @Test
    void search2dDemo() {
        List<Step> steps = search2dService.search2d(SEARCH_MATRIX,
                List.of(3));
        assertEquals(6, steps.size());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0, 11), steps.get(0).getData());
        assertEquals(List.of(), steps.get(0).getHighlight());
        int[][] probes = {{0, 11, 5, 11}, {0, 4, 2, 5}, {0, 1, 0, 1},
                {1, 1, 1, 3}};
        int[] hl = {5, 2, 0, 1};
        for (int k = 0; k < 4; k++) {
            Step step = steps.get(1 + k);
            assertEquals(ints(probes[k][0], probes[k][1], probes[k][2],
                    probes[k][3]), step.getData(), "probe " + k);
            assertEquals(List.of(hl[k]), step.getHighlight(),
                    "probe " + k);
            assertEquals(3, step.getLine(), "probe " + k);
            assertEquals(k == 3 ? List.of(1) : List.of(),
                    step.getSorted(), "probe " + k);
        }
        assertEquals(List.of(1, 1, 1, 3), steps.get(5).getData());
        assertEquals(List.of(), steps.get(5).getHighlight());
        assertEquals(List.of(1), steps.get(5).getSorted());
        assertEquals(0, steps.get(5).getLine());
        assertTrue(steps.get(5).getMessage().contains("Found 3 at (0,1)"));
    }

    @Test
    void search2dDemoNodes() {
        List<Step> steps = search2dService.search2d(SEARCH_MATRIX,
                List.of(3));
        assertEquals(12, steps.get(0).getNodes().size());
        assertNull(steps.get(0).getEdges());
        assertEquals(0, steps.get(0).getNodes().get(0).getId());
        assertEquals(1, steps.get(0).getNodes().get(0).getValue());
        assertEquals("0,0", steps.get(0).getNodes().get(0).getLabel());
        assertEquals(3, steps.get(0).getNodes().get(1).getValue());
        assertEquals("0,1", steps.get(0).getNodes().get(1).getLabel());
        assertEquals(11, steps.get(0).getNodes().get(5).getValue());
        assertEquals("1,1", steps.get(0).getNodes().get(5).getLabel());
        assertEquals(60, steps.get(0).getNodes().get(11).getValue());
        assertEquals("2,3", steps.get(0).getNodes().get(11).getLabel());
    }

    @Test
    void search2dNotFound() {
        List<Step> steps = search2dService.search2d(SEARCH_MATRIX,
                List.of(13));
        assertEquals(5, steps.size());
        assertEquals(List.of(0, 11, 5, 11), steps.get(1).getData());
        assertEquals(List.of(6, 11, 8, 23), steps.get(2).getData());
        assertEquals(List.of(6, 7, 6, 16), steps.get(3).getData());
        assertEquals(List.of(6, 7, 6, 16), steps.get(4).getData());
        assertEquals(List.of(), steps.get(4).getSorted());
        assertEquals(0, steps.get(4).getLine());
        assertTrue(steps.get(4).getMessage()
                .contains("13 is not in the matrix."));
    }

    @Test
    void search2dFirstElement() {
        List<Step> steps = search2dService.search2d(SEARCH_MATRIX,
                List.of(1));
        assertEquals(5, steps.size());
        assertEquals(List.of(0, 1, 0, 1), steps.get(4).getData());
        assertEquals(List.of(0), steps.get(4).getSorted());
    }

    @Test
    void search2dBadInput() {
        List<Step> bad = search2dService.search2d(null, List.of(3));
        assertEquals(1, bad.size());
        assertEquals(0, bad.get(0).getLine());
        List<Step> noTarget = search2dService.search2d(SEARCH_MATRIX,
                null);
        assertEquals(1, noTarget.size());
        List<Step> twoTargets = search2dService.search2d(SEARCH_MATRIX,
                List.of(3, 5));
        assertEquals(1, twoTargets.size());
    }

    // ---- Set Matrix Zeroes ----

    @Test
    void setZeroesDemo() {
        List<Step> steps = setZeroesService.setZeroes(ZEROES_MATRIX);
        assertEquals(5, steps.size());
        int[] build = {1, 1, 1, 1, 0, 1, 1, 1, 1};
        int[] mark = {1, 0, 1, 0, 0, 1, 1, 1, 1};
        int[] zeroRow = {1, 0, 1, 0, 0, 0, 1, 1, 1};
        int[] zeroCol = {1, 0, 1, 0, 0, 0, 1, 0, 1};
        int[][] values = {build, mark, zeroRow, zeroCol, zeroCol};
        for (int s = 0; s < 5; s++) {
            for (int i = 0; i < 9; i++) {
                assertEquals(values[s][i],
                        steps.get(s).getNodes().get(i).getValue(),
                        "step " + s + " node " + i);
            }
        }
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(), steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(1, 1), steps.get(1).getData());
        assertEquals(List.of(4, 1, 3), steps.get(1).getHighlight());
        assertEquals(2, steps.get(1).getLine());
        assertEquals(List.of(0, 1), steps.get(2).getData());
        assertEquals(List.of(3, 4, 5), steps.get(2).getHighlight());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(1, 1), steps.get(3).getData());
        assertEquals(List.of(1, 4, 7), steps.get(3).getHighlight());
        assertEquals(3, steps.get(3).getLine());
        assertEquals(List.of(1), steps.get(4).getData());
        assertEquals(List.of(), steps.get(4).getHighlight());
        assertEquals(0, steps.get(4).getLine());
        assertNull(steps.get(0).getEdges());
        assertEquals("1,1", steps.get(0).getNodes().get(4).getLabel());
    }

    @Test
    void setZeroesRowZero() {
        List<Step> steps = setZeroesService.setZeroes("0,1,2;3,4,5;6,7,8");
        assertEquals(5, steps.size());
        assertEquals(List.of(0, 0), steps.get(1).getData());
        assertEquals(List.of(0), steps.get(1).getHighlight());
        assertTrue(steps.get(1).getMessage().contains("marked by flag"));
        assertEquals(List.of(0, 0), steps.get(2).getData());
        assertEquals(List.of(0, 1, 2), steps.get(2).getHighlight());
        assertEquals(List.of(1, 0), steps.get(3).getData());
        assertEquals(List.of(0, 3, 6), steps.get(3).getHighlight());
        assertEquals(List.of(1), steps.get(4).getData());
        int[] fin = {0, 0, 0, 0, 4, 5, 0, 7, 8};
        for (int i = 0; i < 9; i++) {
            assertEquals(fin[i], steps.get(4).getNodes().get(i).getValue(),
                    "node " + i);
        }
    }

    @Test
    void setZeroesTwoZeroes() {
        List<Step> steps = setZeroesService.setZeroes("1,0,1;1,1,1;0,1,1");
        assertEquals(8, steps.size());
        assertEquals(List.of(0, 1), steps.get(1).getData());
        assertEquals(List.of(1), steps.get(1).getHighlight());
        assertEquals(List.of(2, 0), steps.get(2).getData());
        assertEquals(List.of(6), steps.get(2).getHighlight());
        assertEquals(List.of(0, 0), steps.get(3).getData());
        assertEquals(List.of(0, 2), steps.get(4).getData());
        assertEquals(List.of(1, 0), steps.get(5).getData());
        assertEquals(List.of(1, 1), steps.get(6).getData());
        assertEquals(List.of(2), steps.get(7).getData());
        int[] fin = {0, 0, 0, 0, 0, 1, 0, 0, 0};
        for (int i = 0; i < 9; i++) {
            assertEquals(fin[i], steps.get(7).getNodes().get(i).getValue(),
                    "node " + i);
        }
    }

    @Test
    void setZeroesBadInput() {
        List<Step> bad = setZeroesService.setZeroes("1,2;3");
        assertEquals(1, bad.size());
        assertEquals(0, bad.get(0).getLine());
        List<Step> none = setZeroesService.setZeroes("1,2;3,4");
        assertEquals(2, none.size());
        assertEquals(List.of(0), none.get(1).getData());
    }

    // ---- Word Search ----

    @Test
    void wordSearchDemo() {
        List<Step> steps = wordSearchService.wordSearch(WORD_BOARD,
                List.of("ABCCED"));
        assertEquals(11, steps.size());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getData());
        int[][] exams = {{0, 1}, {1, 1}, {2, 1}, {3, 0}, {3, 1},
                {4, 0}, {4, 1}, {5, 0}, {5, 1}};
        int[] hl = {0, 1, 2, 3, 6, 7, 10, 11, 9};
        int[][] sorted = {{0}, {0, 1}, {0, 1, 2}, {0, 1, 2},
                {0, 1, 2, 6}, {0, 1, 2, 6}, {0, 1, 2, 6, 10},
                {0, 1, 2, 6, 10}, {0, 1, 2, 6, 10, 9}};
        for (int k = 0; k < 9; k++) {
            Step step = steps.get(1 + k);
            assertEquals(ints(exams[k][0], exams[k][1]),
                    step.getData(), "exam " + k);
            assertEquals(List.of(hl[k]), step.getHighlight(),
                    "exam " + k);
            assertEquals(ints(sorted[k]), step.getSorted(), "exam " + k);
            assertEquals(3, step.getLine(), "exam " + k);
        }
        assertEquals(List.of(6), steps.get(10).getData());
        assertEquals(List.of(), steps.get(10).getHighlight());
        assertEquals(List.of(0, 1, 2, 6, 10, 9),
                steps.get(10).getSorted());
        assertEquals(0, steps.get(10).getLine());
        assertTrue(steps.get(10).getMessage().contains("Found \"ABCCED\""));
    }

    @Test
    void wordSearchDemoNodes() {
        List<Step> steps = wordSearchService.wordSearch(WORD_BOARD,
                List.of("ABCCED"));
        assertEquals(12, steps.get(0).getNodes().size());
        assertNull(steps.get(0).getEdges());
        assertEquals(65, steps.get(0).getNodes().get(0).getValue());
        assertEquals("A", steps.get(0).getNodes().get(0).getLabel());
        assertEquals(67, steps.get(0).getNodes().get(2).getValue());
        assertEquals("C", steps.get(0).getNodes().get(2).getLabel());
        assertEquals(68, steps.get(0).getNodes().get(9).getValue());
        assertEquals("D", steps.get(0).getNodes().get(9).getLabel());
    }

    @Test
    void wordSearchNotFound() {
        List<Step> steps = wordSearchService.wordSearch(WORD_BOARD,
                List.of("ABCF"));
        assertEquals(12, steps.size());
        int[][] exams = {{0, 1}, {1, 1}, {2, 1}, {3, 0}, {3, 0},
                {2, 0}, {1, 0}, {0, 1}, {1, 0}, {1, 0}};
        int[] hl = {0, 1, 2, 3, 6, 5, 4, 8, 4, 9};
        int[][] sorted = {{0}, {0, 1}, {0, 1, 2}, {0, 1, 2},
                {0, 1, 2}, {0, 1}, {0}, {8}, {8}, {8}};
        for (int k = 0; k < 10; k++) {
            Step step = steps.get(1 + k);
            assertEquals(ints(exams[k][0], exams[k][1]),
                    step.getData(), "exam " + k);
            assertEquals(List.of(hl[k]), step.getHighlight(),
                    "exam " + k);
            assertEquals(ints(sorted[k]), step.getSorted(), "exam " + k);
        }
        assertEquals(List.of(0), steps.get(11).getData());
        assertEquals(List.of(), steps.get(11).getSorted());
        assertTrue(steps.get(11).getMessage()
                .contains("\"ABCF\" is not in the board."));
    }

    @Test
    void wordSearchSingleLetter() {
        List<Step> steps = wordSearchService.wordSearch(WORD_BOARD,
                List.of("A"));
        assertEquals(3, steps.size());
        assertEquals(List.of(0, 1), steps.get(1).getData());
        assertEquals(List.of(0), steps.get(1).getHighlight());
        assertEquals(List.of(1), steps.get(2).getData());
        assertEquals(List.of(0), steps.get(2).getSorted());
    }

    @Test
    void wordSearchBadInput() {
        List<Step> bad = wordSearchService.wordSearch(null,
                List.of("AB"));
        assertEquals(1, bad.size());
        assertEquals(0, bad.get(0).getLine());
        List<Step> noWord = wordSearchService.wordSearch(WORD_BOARD,
                null);
        assertEquals(1, noWord.size());
        List<Step> lower = wordSearchService.wordSearch(WORD_BOARD,
                List.of("ab"));
        assertEquals(1, lower.size());
        List<Step> lowerBoard = wordSearchService.wordSearch(
                "a,b,c;d,e,f", List.of("AB"));
        assertEquals(1, lowerBoard.size());
    }

    // ---- 400s (controller validators) ----

    
}
