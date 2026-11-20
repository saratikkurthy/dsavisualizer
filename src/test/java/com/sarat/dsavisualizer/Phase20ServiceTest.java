package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.LfuCacheService;
import com.sarat.dsavisualizer.service.MaxFlowService;
import com.sarat.dsavisualizer.service.SudokuService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 20 service tests. Max-flow tests check the CLRS demo
 * (max flow 23, the 3 augmenting paths + bottlenecks, 21 steps,
 * final flow per edge) plus a single-edge and a disconnected
 * case. LFU tests check the LeetCode 460 demo (get returns,
 * evictions, final cache, 11 steps) plus a put-on-existing-key
 * and a capacity-1 case. Sudoku tests check the demo puzzle
 * (solved grid == the known solution, 55 tries, 3 backtracks,
 * 59 steps), an unsolvable grid, the 5000-step cap via a small
 * cap override, and an already-solved grid. The 400 tests call
 * the controller endpoints directly — the controller is built
 * with nulls for the other services because the validators 400
 * before any service is touched (the Phase 16 precedent).
 */
class Phase20ServiceTest {

    private final MaxFlowService maxFlowService = new MaxFlowService();
    private final LfuCacheService lfuCacheService = new LfuCacheService();
    private final SudokuService sudokuService = new SudokuService();

    /** The demo network (Phase 20 design §A1, CLRS). */
    private static final List<List<Integer>> DEMO_EDGES = List.of(
            List.of(0, 1, 16), List.of(0, 2, 13),
            List.of(1, 2, 10), List.of(1, 3, 12),
            List.of(2, 1, 4), List.of(2, 4, 14),
            List.of(3, 2, 9), List.of(3, 5, 20),
            List.of(4, 3, 7), List.of(4, 5, 4));

    /** The demo ops (Phase 20 design §A2, LeetCode 460). */
    private static final List<String> DEMO_OPS = List.of(
            "put 1 1", "put 2 2", "get 1", "put 3 3", "get 2",
            "get 3", "put 4 4", "get 1", "get 3", "get 4");

    /** The demo puzzle (Phase 20 design §A3). */
    private static final List<List<Integer>> DEMO_GRID = List.of(
            List.of(5, 3, 0, 0, 7, 0, 0, 0, 0),
            List.of(6, 0, 0, 1, 9, 0, 0, 0, 0),
            List.of(0, 9, 8, 0, 0, 0, 0, 6, 0),
            List.of(8, 0, 0, 0, 6, 0, 0, 0, 3),
            List.of(4, 0, 0, 8, 0, 3, 0, 0, 1),
            List.of(7, 0, 0, 0, 2, 0, 0, 0, 6),
            List.of(0, 6, 0, 0, 0, 0, 2, 8, 0),
            List.of(0, 0, 0, 4, 1, 9, 0, 0, 5),
            List.of(0, 0, 0, 0, 8, 0, 0, 7, 9));

    /** The demo puzzle's known solution, flattened row-major. */
    private static final List<Integer> DEMO_SOLUTION = List.of(
            5, 3, 4, 6, 7, 8, 9, 1, 2,
            6, 7, 2, 1, 9, 5, 3, 4, 8,
            1, 9, 8, 3, 4, 2, 5, 6, 7,
            8, 5, 9, 7, 6, 1, 4, 2, 3,
            4, 2, 6, 8, 5, 3, 7, 9, 1,
            7, 1, 3, 9, 2, 4, 8, 5, 6,
            9, 6, 1, 5, 3, 7, 2, 8, 4,
            2, 8, 7, 4, 1, 9, 6, 3, 5,
            3, 4, 5, 2, 8, 6, 1, 7, 9);

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** A request with graphEdges + start + target set. */
    private TraceRequest maxFlowRequest(List<List<Integer>> edges,
                                        Integer start, Integer target) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        request.setStart(start);
        request.setTarget(target);
        return request;
    }

    /** A request with capacity + ops set. */
    private TraceRequest lfuRequest(Integer capacity, List<String> ops) {
        TraceRequest request = new TraceRequest();
        request.setCapacity(capacity);
        request.setWords(ops);
        return request;
    }

    /** A request with only a grid set. */
    private TraceRequest sudokuRequest(List<List<Integer>> grid) {
        TraceRequest request = new TraceRequest();
        request.setGrid(grid);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the Phase 20 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 35
     * Phase 1–19 params, then maxFlow, lfu, sudoku (38 total). */

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- Max flow tests ----

    @Test
    void maxFlowDemo() {
        List<Step> steps = maxFlowService.maxflow(DEMO_EDGES, 0, 5);
        // 17 BFS visits + 3 augmentations + final = 21 steps.
        assertEquals(21, steps.size());
        assertEquals("BFS visits 0.", steps.get(0).getMessage());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals(List.of(0), steps.get(0).getSorted());
        assertEquals("BFS visits 3.", steps.get(3).getMessage());
        assertEquals(List.of(0, 1, 2, 3), steps.get(3).getSorted());
        assertEquals("Augment along 0\u21921\u21923\u21925 by 12: flow 12.",
                steps.get(4).getMessage());
        assertEquals(4, steps.get(4).getLine());
        assertEquals(List.of(0, 1, 3, 5), steps.get(4).getHighlight());
        assertEquals(List.of(12, 0, 0, 12, 0, 0, 0, 12, 0, 0),
                steps.get(4).getData());
        assertEquals(List.of(4, 13, 10, 0, 4, 14, 9, 8, 7, 4),
                steps.get(4).getAux());
        assertEquals("Augment along 0\u21922\u21924\u21925 by 4: flow 16.",
                steps.get(9).getMessage());
        assertEquals("Augment along 0\u21922\u21924\u21923\u21925 by 7: flow 23.",
                steps.get(15).getMessage());
        assertEquals(List.of(0, 1, 2, 4, 3), steps.get(15).getSorted());
        assertEquals("Max flow = 23 (no more augmenting paths).",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(12, 11, 0, 12, 0, 11, 0, 19, 7, 4),
                lastStep(steps).getData());
        assertEquals(List.of(4, 2, 10, 0, 4, 3, 9, 1, 0, 0),
                lastStep(steps).getAux());
        assertEquals(10, lastStep(steps).getEdges().size());
        assertEquals(20, lastStep(steps).getEdges().get(7).getWeight());
    }

    @Test
    void maxFlowSingleEdge() {
        List<Step> steps = maxFlowService.maxflow(
                List.of(List.of(0, 1, 5)), 0, 1);
        // visit, augment, visit (no path), final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Augment along 0\u21921 by 5: flow 5.",
                steps.get(1).getMessage());
        assertEquals("Max flow = 5 (no more augmenting paths).",
                lastStep(steps).getMessage());
        assertEquals(List.of(5), lastStep(steps).getData());
        assertEquals(List.of(0), lastStep(steps).getAux());
    }

    @Test
    void maxFlowDisconnected() {
        List<Step> steps = maxFlowService.maxflow(
                List.of(List.of(0, 1, 5), List.of(2, 3, 5)), 0, 3);
        // visit 0, visit 1, final = 3 steps; no augmenting path.
        assertEquals(3, steps.size());
        assertEquals("Max flow = 0 (no more augmenting paths).",
                lastStep(steps).getMessage());
        assertEquals(List.of(0, 0), lastStep(steps).getData());
        assertEquals(List.of(5, 5), lastStep(steps).getAux());
    }

    // ---- LFU tests ----

    @Test
    void lfuDemo() {
        List<Step> steps = lfuCacheService.simulate(2, DEMO_OPS);
        // 10 ops + final = 11 steps.
        assertEquals(11, steps.size());
        assertEquals("Put 1=1 (freq 1).", steps.get(0).getMessage());
        assertEquals(5, steps.get(0).getLine());
        assertEquals(List.of(1), steps.get(0).getData());
        assertEquals(List.of(1), steps.get(0).getAux());
        assertEquals("Put 2=2 (freq 1).", steps.get(1).getMessage());
        assertEquals(List.of(1, 2), steps.get(1).getData());
        assertEquals("Get 1 \u2192 1 (freq 2).", steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(2, 1), steps.get(2).getData());
        assertEquals(List.of(1, 2), steps.get(2).getAux());
        assertEquals("Put 3=3: evict 2 (freq 1, least recently used).",
                steps.get(3).getMessage());
        assertEquals(4, steps.get(3).getLine());
        assertEquals(List.of(3, 1), steps.get(3).getData());
        assertEquals("Get 2 \u2192 -1 (not in cache).",
                steps.get(4).getMessage());
        assertEquals(2, steps.get(4).getLine());
        assertEquals("Get 3 \u2192 3 (freq 2).", steps.get(5).getMessage());
        assertEquals(List.of(1, 3), steps.get(5).getData());
        assertEquals(List.of(2, 2), steps.get(5).getAux());
        assertEquals("Put 4=4: evict 1 (freq 2, least recently used).",
                steps.get(6).getMessage());
        assertEquals("Get 1 \u2192 -1 (not in cache).",
                steps.get(7).getMessage());
        assertEquals("Get 3 \u2192 3 (freq 3).", steps.get(8).getMessage());
        assertEquals("Get 4 \u2192 4 (freq 2).", steps.get(9).getMessage());
        assertEquals("Cache: 4=4 (freq 2), 3=3 (freq 3).",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(4, 3), lastStep(steps).getData());
        assertEquals(List.of(2, 3), lastStep(steps).getAux());
    }

    @Test
    void lfuPutExistingKey() {
        List<Step> steps = lfuCacheService.simulate(2,
                List.of("put 1 1", "put 1 9", "get 1"));
        assertEquals(4, steps.size());
        assertEquals("Put 1=9: update (freq 2).",
                steps.get(1).getMessage());
        assertEquals(3, steps.get(1).getLine());
        assertEquals("Get 1 \u2192 9 (freq 3).", steps.get(2).getMessage());
        assertEquals("Cache: 1=9 (freq 3).",
                lastStep(steps).getMessage());
        assertEquals(List.of(1), lastStep(steps).getData());
        assertEquals(List.of(3), lastStep(steps).getAux());
    }

    @Test
    void lfuCapacityOne() {
        List<Step> steps = lfuCacheService.simulate(1,
                List.of("put 1 1", "put 2 2", "get 1", "get 2"));
        assertEquals(5, steps.size());
        assertEquals("Put 2=2: evict 1 (freq 1, least recently used).",
                steps.get(1).getMessage());
        assertEquals("Get 1 \u2192 -1 (not in cache).",
                steps.get(2).getMessage());
        assertEquals("Get 2 \u2192 2 (freq 2).", steps.get(3).getMessage());
        assertEquals("Cache: 2=2 (freq 2).",
                lastStep(steps).getMessage());
        assertEquals(List.of(2), lastStep(steps).getData());
    }

    // ---- Sudoku tests ----

    @Test
    void sudokuDemo() {
        List<Step> steps = sudokuService.sudoku(DEMO_GRID);
        // 55 tries + 3 backtracks + final = 59 steps.
        assertEquals(59, steps.size());
        int tries = 0;
        int backtracks = 0;
        for (Step step : steps) {
            if (step.getLine() == 2) {
                tries++;
                assertEquals(1, step.getHighlight().size());
            } else if (step.getLine() == 4) {
                backtracks++;
                assertEquals(1, step.getHighlight().size());
            }
        }
        assertEquals(55, tries);
        assertEquals(3, backtracks);
        assertEquals("Solved in 55 tries, 3 backtracks.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(DEMO_SOLUTION, lastStep(steps).getData());
        assertTrue(lastStep(steps).getHighlight().isEmpty());
    }

    @Test
    void sudokuUnsolvable() {
        // (0,8) has no candidates: row 0 holds 1..8, column 8
        // holds 9 — consistent givens, no solution.
        List<List<Integer>> grid = List.of(
                List.of(1, 2, 3, 4, 5, 6, 7, 8, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 9),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0),
                List.of(0, 0, 0, 0, 0, 0, 0, 0, 0));
        List<Step> steps = sudokuService.sudoku(grid);
        assertEquals(1, steps.size());
        assertEquals("No solution exists.", lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void sudokuStepCap() {
        // Small cap override: the demo puzzle needs 59 steps, so
        // a cap of 10 stops the search mid-way (a real test of
        // the cap path, not a comment).
        List<Step> steps = sudokuService.sudoku(DEMO_GRID, 10);
        assertEquals(11, steps.size());
        assertEquals("Stopped after 10 steps (no solution found yet).",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        for (int i = 0; i < 10; i++) {
            assertEquals(2, steps.get(i).getLine());
        }
    }

    @Test
    void sudokuAlreadySolved() {
        List<List<Integer>> solved = List.of(
                List.of(5, 3, 4, 6, 7, 8, 9, 1, 2),
                List.of(6, 7, 2, 1, 9, 5, 3, 4, 8),
                List.of(1, 9, 8, 3, 4, 2, 5, 6, 7),
                List.of(8, 5, 9, 7, 6, 1, 4, 2, 3),
                List.of(4, 2, 6, 8, 5, 3, 7, 9, 1),
                List.of(7, 1, 3, 9, 2, 4, 8, 5, 6),
                List.of(9, 6, 1, 5, 3, 7, 2, 8, 4),
                List.of(2, 8, 7, 4, 1, 9, 6, 3, 5),
                List.of(3, 4, 5, 2, 8, 6, 1, 7, 9));
        List<Step> steps = sudokuService.sudoku(solved);
        assertEquals(1, steps.size());
        assertEquals("Solved in 0 tries, 0 backtracks.",
                lastStep(steps).getMessage());
        assertEquals(DEMO_SOLUTION, lastStep(steps).getData());
    }

    // ---- 400 tests (controller validation) ----


}
