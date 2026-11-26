package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.HomeController;
import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.DiagonalTraverseService;
import com.sarat.dsavisualizer.service.DinicService;
import com.sarat.dsavisualizer.service.JohnsonService;
import com.sarat.dsavisualizer.service.MaximalSquareService;
import com.sarat.dsavisualizer.service.StoerWagnerService;
import com.sarat.dsavisualizer.service.TransposeService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 31 service tests. Dinic's tests check the demo graph
 * (15 steps, 2 phases, max flow 19, final flow per edge
 * [10,9,0,4,6,9,9,5,10] — BFS visit steps carry the level
 * array in `table` as discovered so far, augment steps the
 * phase's complete levels, and the final step the failing
 * BFS's levels [0,-1,1,-1,-1,-1]). Johnson's tests check the
 * demo graph (21 steps, h = [0,0,-2,0], all-pairs matrix
 * rows [0,1,-1,2]/[2,0,-2,1]/[4,5,0,3]/[1,2,0,0] — the
 * service is DIRECTED, unlike Floyd-Warshall). Stoer-Wagner
 * tests check the demo graph (14 steps, phase cuts 5, 8, 5,
 * min cut 5, partition {3}|{0,1,2} — merged supernodes are
 * labelled "2,3" / "1,2,3", singletons keep a null label).
 * Transpose tests check the demo 2x3 "1,2,3;4,5,6" (8 steps,
 * 3x2 result [[1,4],[2,5],[3,6]]). Diagonal Traverse tests
 * check the demo 3x3 (11 steps, order [1,2,4,7,5,3,6,8,9]).
 * Maximal Square tests check the LeetCode 221 demo (22 steps,
 * dp table pinned cell by cell, max side 2, area 4, best
 * cells [7,8,12,13]). The 400 tests call the controller
 * endpoints directly — the controller is built with nulls
 * for the other services because the validators 400 before
 * any service is touched (the Phase 16 precedent).
 */
class Phase31ServiceTest {

    private final DinicService dinicService = new DinicService();
    private final JohnsonService johnsonService = new JohnsonService();
    private final StoerWagnerService stoerWagnerService =
            new StoerWagnerService();
    private final TransposeService transposeService = new TransposeService();
    private final DiagonalTraverseService diagonalTraverseService =
            new DiagonalTraverseService();
    private final MaximalSquareService maximalSquareService =
            new MaximalSquareService();

    static final List<List<Integer>> DINIC_EDGES = List.of(
            List.of(0, 1, 10), List.of(0, 2, 10), List.of(1, 2, 2),
            List.of(1, 3, 4), List.of(1, 4, 8), List.of(2, 4, 9),
            List.of(3, 5, 10), List.of(4, 3, 6), List.of(4, 5, 10));
    static final List<List<Integer>> JOHNSON_EDGES = List.of(
            List.of(0, 1, 1), List.of(0, 2, 4), List.of(1, 2, -2),
            List.of(1, 3, 2), List.of(2, 3, 3), List.of(3, 0, 1));
    static final List<List<Integer>> SW_EDGES = List.of(
            List.of(0, 1, 3), List.of(0, 2, 2), List.of(1, 2, 4),
            List.of(1, 3, 2), List.of(2, 3, 3));
    static final String TRANSPOSE_MATRIX = "1,2,3;4,5,6";
    static final String DIAGONAL_MATRIX = "1,2,3;4,5,6;7,8,9";
    static final String MAXIMAL_MATRIX =
            "1,0,1,0,0;1,0,1,1,1;1,1,1,1,1;1,0,0,1,0";

    static VisualizationController controller() {
        return new VisualizationController(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                new DinicService(), new JohnsonService(),
                new StoerWagnerService(), new TransposeService(),
                new DiagonalTraverseService(),
                new MaximalSquareService());
    }

    static TraceRequest graphRequest(List<List<Integer>> edges,
                                     Integer start, Integer target) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setStart(start);
        request.setTarget(target);
        return request;
    }

    static TraceRequest graphRequest(List<List<Integer>> edges) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        return request;
    }

    static TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    static List<Integer> ints(int... values) {
        List<Integer> out = new ArrayList<>();
        for (int v : values) {
            out.add(v);
        }
        return out;
    }

    // ---- Dinic's max flow ----

    @Test
    void dinicDemoStepCount() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        assertEquals(15, steps.size());
    }

    @Test
    void dinicDemoLines() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        int[] lines = {2, 2, 2, 2, 4, 4, 4, 2, 2, 2, 2, 4, 2, 2, 0};
        for (int i = 0; i < 15; i++) {
            assertEquals(lines[i], steps.get(i).getLine());
        }
    }

    @Test
    void dinicFinalFlow() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        Step last = steps.get(14);
        assertEquals(Arrays.asList(10, 9, 0, 4, 6, 9, 9, 5, 10),
                last.getData());
        assertTrue(last.getMessage().contains("19"));
        assertEquals(List.of(0, 2), last.getSorted());
    }

    @Test
    void dinicFinalResidual() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        assertEquals(Arrays.asList(0, 1, 2, 0, 2, 0, 1, 1, 0),
                steps.get(14).getAux());
    }

    @Test
    void dinicPhaseLevels() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        assertEquals(List.of(Arrays.asList(0, 1, 1, 2, 2, 3)),
                steps.get(4).getTable());
        assertEquals(List.of(Arrays.asList(0, -1, 1, 3, 2, 4)),
                steps.get(11).getTable());
        assertEquals(List.of(Arrays.asList(0, -1, -1, -1, -1, -1)),
                steps.get(0).getTable());
        assertEquals(List.of(Arrays.asList(0, 1, 1, 2, 2, -1)),
                steps.get(3).getTable());
        assertEquals(List.of(Arrays.asList(0, -1, 1, -1, -1, -1)),
                steps.get(14).getTable());
    }

    @Test
    void dinicAugmentFlows() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        assertEquals(Arrays.asList(4, 0, 0, 4, 0, 0, 4, 0, 0),
                steps.get(4).getData());
        assertEquals(Arrays.asList(10, 0, 0, 4, 6, 0, 4, 0, 6),
                steps.get(5).getData());
        assertEquals(Arrays.asList(10, 4, 0, 4, 6, 4, 4, 0, 10),
                steps.get(6).getData());
        assertEquals(Arrays.asList(10, 9, 0, 4, 6, 9, 9, 5, 10),
                steps.get(11).getData());
    }

    @Test
    void dinicAugmentMessagesAndHighlights() {
        List<Step> steps = dinicService.dinic(DINIC_EDGES, 0, 5);
        assertTrue(steps.get(4).getMessage()
                .contains("Phase 1: augment along 0\u21921\u21923\u21925 by 4"));
        assertTrue(steps.get(5).getMessage()
                .contains("0\u21921\u21924\u21925 by 6"));
        assertTrue(steps.get(6).getMessage()
                .contains("0\u21922\u21924\u21925 by 4"));
        assertTrue(steps.get(11).getMessage()
                .contains("Phase 2: augment along 0\u21922\u21924\u21923\u21925 by 5"));
        assertEquals(List.of(0, 1, 3, 5), steps.get(4).getHighlight());
        assertEquals(List.of(0, 2, 4, 3, 5), steps.get(11).getHighlight());
        assertEquals(6, steps.get(0).getNodes().size());
        assertEquals(9, steps.get(0).getEdges().size());
        for (int i = 0; i < 6; i++) {
            assertNull(steps.get(0).getNodes().get(i).getLabel());
        }
    }

    @Test
    void dinicSmallAndEdgeCases() {
        List<Step> small = dinicService.dinic(
                List.of(List.of(0, 1, 5)), 0, 1);
        assertEquals(4, small.size());
        assertEquals(List.of(5), small.get(3).getData());
        assertTrue(small.get(3).getMessage().contains("5"));
        List<Step> empty = dinicService.dinic(null, 0, 5);
        assertEquals(1, empty.size());
        assertEquals(0, empty.get(0).getLine());
        List<Step> bad = dinicService.dinic(DINIC_EDGES, 9, 5);
        assertEquals(1, bad.size());
        assertTrue(bad.get(0).getMessage()
                .contains("Source and sink must be nodes"));
    }

    // ---- Johnson's all-pairs shortest paths ----

    @Test
    void johnsonDemoStepCount() {
        assertEquals(21, johnsonService.johnson(JOHNSON_EDGES).size());
    }

    @Test
    void johnsonBellmanFord() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0, 0, 0, 0), steps.get(0).getData());
        assertEquals(3, steps.get(1).getLine());
        assertEquals(List.of(0, 0, -2, 0), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(0, 0, -2, 0), steps.get(2).getData());
        assertEquals(List.of(), steps.get(2).getHighlight());
    }

    @Test
    void johnsonReweight() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(2, steps.get(3).getLine());
        assertEquals(List.of(0, 0, -2, 0), steps.get(3).getData());
        assertEquals(List.of(1, 6, 0, 2, 1, 1), steps.get(3).getAux());
        assertEquals(-2, steps.get(3).getEdges().get(2).getWeight());
        assertEquals(-2, steps.get(20).getEdges().get(2).getWeight());
    }

    @Test
    void johnsonDijkstraSource0() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(Arrays.asList(0, 1, 6, null), steps.get(4).getData());
        assertEquals(Arrays.asList(0, 1, 1, 3), steps.get(5).getData());
        assertEquals(Arrays.asList(0, 1, 1, 2), steps.get(6).getData());
        assertEquals(Arrays.asList(0, 1, 1, 2), steps.get(7).getData());
        assertEquals(List.of(0), steps.get(4).getSorted());
        assertEquals(List.of(0, 1, 2, 3), steps.get(7).getSorted());
    }

    @Test
    void johnsonDijkstraSource1() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(Arrays.asList(null, 0, 0, 2), steps.get(8).getData());
        assertEquals(Arrays.asList(null, 0, 0, 1), steps.get(9).getData());
        assertEquals(Arrays.asList(2, 0, 0, 1), steps.get(10).getData());
        assertEquals(Arrays.asList(2, 0, 0, 1), steps.get(11).getData());
    }

    @Test
    void johnsonDijkstraSource2() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(Arrays.asList(null, null, 0, 1),
                steps.get(12).getData());
        assertEquals(Arrays.asList(2, null, 0, 1), steps.get(13).getData());
        assertEquals(Arrays.asList(2, 3, 0, 1), steps.get(14).getData());
        assertEquals(Arrays.asList(2, 3, 0, 1), steps.get(15).getData());
    }

    @Test
    void johnsonDijkstraSource3AndFinal() {
        List<Step> steps = johnsonService.johnson(JOHNSON_EDGES);
        assertEquals(Arrays.asList(1, null, null, 0),
                steps.get(16).getData());
        assertEquals(Arrays.asList(1, 2, 7, 0), steps.get(17).getData());
        assertEquals(Arrays.asList(1, 2, 2, 0), steps.get(18).getData());
        assertEquals(Arrays.asList(1, 2, 2, 0), steps.get(19).getData());
        assertEquals(0, steps.get(20).getLine());
        assertEquals(Arrays.asList(0, 1, -1, 2, 2, 0, -2, 1, 4, 5, 0, 3,
                        1, 2, 0, 0),
                steps.get(20).getData());
        assertEquals(List.of(0, 1, 2, 3), steps.get(20).getSorted());
        assertEquals(4, steps.get(0).getNodes().size());
        assertEquals(6, steps.get(0).getEdges().size());
    }

    @Test
    void johnsonEdgeCases() {
        List<Step> neg = johnsonService.johnson(List.of(
                List.of(0, 1, 1), List.of(1, 0, -2)));
        assertEquals(3, neg.size());
        assertTrue(neg.get(2).getMessage()
                .contains("Negative cycle detected"));
        List<Step> two = johnsonService.johnson(List.of(
                List.of(0, 1), List.of(1, 2)));
        assertEquals(10, two.size());
        assertEquals(Arrays.asList(0, 1, 2, null, 0, 1, null, null, 0),
                two.get(9).getData());
        List<Step> empty = johnsonService.johnson(null);
        assertEquals(1, empty.size());
    }

    // ---- Stoer-Wagner global min cut ----

    @Test
    void swDemoStepCount() {
        assertEquals(14, stoerWagnerService.stoerWagner(SW_EDGES).size());
    }

    @Test
    void swLinesAndData() {
        List<Step> steps = stoerWagnerService.stoerWagner(SW_EDGES);
        int[] lines = {2, 3, 3, 3, 3, 4, 3, 3, 3, 4, 3, 3, 4, 0};
        int[][] data = {{4}, {0}, {3}, {6}, {5}, {5}, {0}, {3}, {8},
                {8}, {0}, {5}, {5}, {5}};
        for (int i = 0; i < 14; i++) {
            assertEquals(lines[i], steps.get(i).getLine());
            assertEquals(ints(data[i]), steps.get(i).getData());
        }
    }

    @Test
    void swCutsAndFinal() {
        List<Step> steps = stoerWagnerService.stoerWagner(SW_EDGES);
        assertEquals(List.of(1), steps.get(2).getHighlight());
        assertEquals(List.of(0, 1, 2), steps.get(3).getSorted());
        assertEquals(List.of(0, 1, 2, 3), steps.get(5).getSorted());
        assertEquals(List.of(0, 1, 2), steps.get(9).getSorted());
        assertEquals(List.of(0, 1), steps.get(12).getSorted());
        assertEquals(List.of(3), steps.get(13).getSorted());
        assertTrue(steps.get(13).getMessage()
                .contains("Minimum cut = 5"));
    }

    @Test
    void swMergedNodes() {
        List<Step> steps = stoerWagnerService.stoerWagner(SW_EDGES);
        assertEquals(3, steps.get(6).getNodes().size());
        assertEquals("2,3", steps.get(8).getNodes().get(2).getLabel());
        assertEquals(2, steps.get(8).getNodes().get(2).getId());
        assertEquals(2, steps.get(8).getNodes().get(2).getValue());
        assertNull(steps.get(8).getNodes().get(0).getLabel());
        assertEquals(3, steps.get(8).getEdges().size());
        assertEquals(6, steps.get(8).getEdges().get(2).getWeight());
        assertEquals("1,2,3", steps.get(11).getNodes().get(1).getLabel());
    }

    @Test
    void swTwoNode() {
        List<Step> steps = stoerWagnerService.stoerWagner(
                List.of(List.of(0, 1, 7)));
        assertEquals(5, steps.size());
        assertEquals(List.of(7), steps.get(4).getData());
        assertEquals(List.of(0), steps.get(4).getSorted());
    }

    @Test
    void swEmpty() {
        assertEquals(1, stoerWagnerService.stoerWagner(null).size());
    }

    // ---- Transpose ----

    @Test
    void transposeDemoSteps() {
        List<Step> steps = transposeService.transpose(TRANSPOSE_MATRIX);
        assertEquals(8, steps.size());
        assertEquals(List.of(2, 3), steps.get(0).getData());
        assertEquals(6, steps.get(0).getNodes().size());
        assertEquals(0, steps.get(0).getNodes().get(0).getValue());
        int[] hl = {0, 2, 4, 1, 3, 5};
        int[][] sorted = {{0}, {0, 2}, {0, 2, 4}, {0, 2, 4, 1},
                {0, 2, 4, 1, 3}, {0, 2, 4, 1, 3, 5}};
        for (int k = 0; k < 6; k++) {
            Step step = steps.get(1 + k);
            assertEquals(List.of(k + 1), step.getData());
            assertEquals(List.of(hl[k]), step.getHighlight());
            assertEquals(ints(sorted[k]), step.getSorted());
            assertEquals(3, step.getLine());
        }
    }

    @Test
    void transposeFinal() {
        List<Step> steps = transposeService.transpose(TRANSPOSE_MATRIX);
        assertEquals(List.of(3, 2), steps.get(7).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5), steps.get(7).getSorted());
        assertEquals(0, steps.get(7).getLine());
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            values.add(steps.get(7).getNodes().get(i).getValue());
        }
        assertEquals(List.of(1, 4, 2, 5, 3, 6), values);
        assertEquals("2,1", steps.get(7).getNodes().get(5).getLabel());
    }

    @Test
    void transposeOneByOne() {
        List<Step> steps = transposeService.transpose("5");
        assertEquals(3, steps.size());
        assertEquals(List.of(1, 1), steps.get(2).getData());
    }

    @Test
    void transposeBad() {
        assertEquals(1, transposeService.transpose(null).size());
    }

    // ---- Diagonal traverse ----

    @Test
    void diagonalDemoSteps() {
        List<Step> steps = diagonalTraverseService.diagonalTraverse(
                DIAGONAL_MATRIX);
        assertEquals(11, steps.size());
        assertEquals(List.of(0, 0), steps.get(0).getData());
        int[][] data = {{0, 1}, {1, 2}, {2, 4}, {3, 7}, {4, 5},
                {5, 3}, {6, 6}, {7, 8}, {8, 9}};
        int[] hl = {0, 1, 3, 6, 4, 2, 5, 7, 8};
        for (int k = 0; k < 9; k++) {
            Step step = steps.get(1 + k);
            assertEquals(ints(data[k][0], data[k][1]), step.getData());
            assertEquals(List.of(hl[k]), step.getHighlight());
            assertEquals(3, step.getLine());
        }
    }

    @Test
    void diagonalFinal() {
        List<Step> steps = diagonalTraverseService.diagonalTraverse(
                DIAGONAL_MATRIX);
        assertEquals(List.of(9), steps.get(10).getData());
        assertEquals(List.of(0, 1, 3, 6, 4, 2, 5, 7, 8),
                steps.get(10).getSorted());
    }

    @Test
    void diagonalRectangular() {
        List<Step> steps = diagonalTraverseService.diagonalTraverse(
                "1,2,3;4,5,6");
        assertEquals(8, steps.size());
        List<Integer> order = new ArrayList<>();
        for (int k = 1; k <= 6; k++) {
            order.add(steps.get(k).getData().get(1));
        }
        assertEquals(List.of(1, 2, 4, 5, 3, 6), order);
    }

    @Test
    void diagonalOneByOneAndBad() {
        List<Step> one = diagonalTraverseService.diagonalTraverse("7");
        assertEquals(3, one.size());
        assertEquals(List.of(0, 7), one.get(1).getData());
        assertEquals(1, diagonalTraverseService.diagonalTraverse(null).size());
    }

    // ---- Maximal square ----

    @Test
    void maximalDemoStepCount() {
        List<Step> steps = maximalSquareService.maximalSquare(MAXIMAL_MATRIX);
        assertEquals(22, steps.size());
        assertEquals(List.of(0, 0), steps.get(0).getData());
    }

    @Test
    void maximalDpTable() {
        List<Step> steps = maximalSquareService.maximalSquare(MAXIMAL_MATRIX);
        int[][] data = {
                {0, 0, 1, 1}, {0, 1, 0, 1}, {0, 2, 1, 1}, {0, 3, 0, 1},
                {0, 4, 0, 1}, {1, 0, 1, 1}, {1, 1, 0, 1}, {1, 2, 1, 1},
                {1, 3, 1, 1}, {1, 4, 1, 1}, {2, 0, 1, 1}, {2, 1, 1, 1},
                {2, 2, 1, 1}, {2, 3, 2, 2}, {2, 4, 2, 2}, {3, 0, 1, 2},
                {3, 1, 0, 2}, {3, 2, 0, 2}, {3, 3, 1, 2}, {3, 4, 0, 2}};
        for (int k = 0; k < 20; k++) {
            Step step = steps.get(1 + k);
            assertEquals(
                    ints(data[k][0], data[k][1], data[k][2], data[k][3]),
                    step.getData());
            assertEquals(3, step.getLine());
        }
    }

    @Test
    void maximalBestSquare() {
        List<Step> steps = maximalSquareService.maximalSquare(MAXIMAL_MATRIX);
        assertEquals(List.of(7, 8, 12, 13), steps.get(14).getSorted());
        assertEquals(List.of(7, 8, 12, 13), steps.get(15).getSorted());
        assertEquals(List.of(2, 4), steps.get(21).getData());
        assertEquals(List.of(7, 8, 12, 13), steps.get(21).getSorted());
        assertEquals(0, steps.get(21).getLine());
    }

    @Test
    void maximalNodesCarryInput() {
        List<Step> steps = maximalSquareService.maximalSquare(MAXIMAL_MATRIX);
        assertEquals(0, steps.get(14).getNodes().get(1).getValue());
    }

    @Test
    void maximalZerosAndBad() {
        List<Step> zeros = maximalSquareService.maximalSquare("0,0;0,0");
        assertEquals(6, zeros.size());
        assertEquals(List.of(0, 0), zeros.get(5).getData());
        assertEquals(List.of(), zeros.get(5).getSorted());
        assertEquals(1, maximalSquareService.maximalSquare("1,2;0,1").size());
    }

    // ---- Controller validators + endpoint count ----

    @Test
    void dinicValidators() {
        VisualizationController c = controller();
        assertEquals(200, c.graphDinic(
                graphRequest(DINIC_EDGES, 0, 5)).getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(List.of(List.of(0, 1, 5)), 0, 1))
                .getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(List.of(List.of(0, 1, 0), List.of(1, 2, 5)),
                        0, 2)).getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(List.of(List.of(0, 1, 100), List.of(1, 2, 5)),
                        0, 2)).getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(DINIC_EDGES, 0, 0)).getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(DINIC_EDGES, 0, 9)).getStatusCode().value());
        assertEquals(400, c.graphDinic(
                graphRequest(DINIC_EDGES, null, 5)).getStatusCode().value());
    }

    @Test
    void johnsonValidators() {
        VisualizationController c = controller();
        assertEquals(200, c.graphJohnson(
                graphRequest(JOHNSON_EDGES)).getStatusCode().value());
        assertEquals(400, c.graphJohnson(
                new TraceRequest()).getStatusCode().value());
        assertEquals(400, c.graphJohnson(
                graphRequest(List.of(List.of(0, 1, 2, 3))))
                .getStatusCode().value());
        assertEquals(400, c.graphJohnson(
                graphRequest(List.of(List.of(0, 10, 1))))
                .getStatusCode().value());
    }

    @Test
    void stoerWagnerValidators() {
        VisualizationController c = controller();
        assertEquals(200, c.graphStoerWagner(
                graphRequest(SW_EDGES)).getStatusCode().value());
        assertEquals(400, c.graphStoerWagner(
                graphRequest(List.of(List.of(0, 1, 7))))
                .getStatusCode().value());
        List<List<Integer>> many = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            many.add(List.of(0, 1, 1));
        }
        assertEquals(400, c.graphStoerWagner(
                graphRequest(many)).getStatusCode().value());
        assertEquals(400, c.graphStoerWagner(
                graphRequest(List.of(List.of(0, 1, 0), List.of(1, 2, 5))))
                .getStatusCode().value());
        assertEquals(400, c.graphStoerWagner(
                graphRequest(List.of(List.of(0, 1, 100), List.of(1, 2, 5))))
                .getStatusCode().value());
        assertEquals(400, c.graphStoerWagner(
                graphRequest(List.of(List.of(0, 1, 3), List.of(2, 3, 4))))
                .getStatusCode().value());
        assertEquals(400, c.graphStoerWagner(
                graphRequest(List.of(List.of(0, 10, 3), List.of(1, 2, 5))))
                .getStatusCode().value());
    }

    @Test
    void matrixValidators() {
        VisualizationController c = controller();
        assertEquals(200, c.matrixTranspose(
                wordRequest(TRANSPOSE_MATRIX)).getStatusCode().value());
        assertEquals(200, c.matrixTranspose(
                wordRequest("5")).getStatusCode().value());
        assertEquals(400, c.matrixTranspose(
                wordRequest("1,2;3")).getStatusCode().value());
        assertEquals(400, c.matrixTranspose(
                wordRequest("1,2;3,4,5")).getStatusCode().value());
        assertEquals(400, c.matrixTranspose(
                wordRequest("1,100;3,4")).getStatusCode().value());
        assertEquals(400, c.matrixTranspose(
                new TraceRequest()).getStatusCode().value());
        assertEquals(200, c.matrixDiagonalTraverse(
                wordRequest(DIAGONAL_MATRIX)).getStatusCode().value());
        assertEquals(200, c.matrixDiagonalTraverse(
                wordRequest("7")).getStatusCode().value());
        assertEquals(400, c.matrixDiagonalTraverse(
                wordRequest("1,2;3")).getStatusCode().value());
        assertEquals(400, c.matrixDiagonalTraverse(
                wordRequest("1,100;3,4")).getStatusCode().value());
    }

    @Test
    void maximalSquareValidatorsAndEndpoints() {
        VisualizationController c = controller();
        assertEquals(200, c.matrixMaximalSquare(
                wordRequest(MAXIMAL_MATRIX)).getStatusCode().value());
        assertEquals(200, c.matrixMaximalSquare(
                wordRequest("0,0;0,0")).getStatusCode().value());
        assertEquals(400, c.matrixMaximalSquare(
                wordRequest("1,2;0,1")).getStatusCode().value());
        assertEquals(400, c.matrixMaximalSquare(
                wordRequest("1,2;3")).getStatusCode().value());
        Set<String> ctrl = new TreeSet<>();
        for (Method m : VisualizationController.class.getDeclaredMethods()) {
            PostMapping pm = m.getAnnotation(PostMapping.class);
            if (pm != null) {
                for (String v : pm.value()) {
                    ctrl.add("/api" + v);
                }
            }
        }
        List<String> home = (List<String>) new HomeController().home()
                .get("endpoints");
        Set<String> homeSet = new TreeSet<>();
        for (String e : home) {
            homeSet.add(e.substring("POST ".length()));
        }
        assertEquals(134, ctrl.size());
        assertEquals(134, homeSet.size());
        assertEquals(ctrl, homeSet);
        for (String p : List.of("/api/graph/dinic",
                "/api/graph/johnson", "/api/graph/stoerwagner",
                "/api/matrix/transpose", "/api/matrix/diagonaltraverse",
                "/api/matrix/maximalsquare")) {
            assertTrue(ctrl.contains(p));
        }
    }
}
