package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.KnapsackService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 10 service tests. Bellman-Ford tests check the final
 * step's distances (node 2 = 1 via the negative edge — Dijkstra
 * would give 2) and the early exit; topological-sort tests check
 * the final step's output order and the cycle message; knapsack
 * tests check the final step's table cell and the taken items in
 * the final message.
 *
 * NOTE: the design doc lists 7 cases; the 8th test here
 * (bellmanFordEarlyExit) was added to pin the early-exit rule —
 * an iteration that relaxes nothing ends the trace, so the
 * example trace has exactly one "Iteration ... complete" summary.
 */
class Phase10ServiceTest {

    private final GraphService graphService = new GraphService();
    private final KnapsackService knapsackService = new KnapsackService();

    /** The Bellman-Ford example (has a negative edge 1-2). */
    private List<List<Integer>> bellmanFordExample() {
        return List.of(
                List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, -3),
                List.of(1, 3, 5), List.of(2, 3, 8), List.of(2, 4, 10),
                List.of(3, 4, 2), List.of(3, 5, 6), List.of(4, 5, 3));
    }

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** Final data of the last step. */
    private List<Integer> lastData(List<Step> steps) {
        return lastStep(steps).getData();
    }

    /** True when any step's message contains the given text. */
    private boolean anyMessageContains(List<Step> steps, String text) {
        for (Step step : steps) {
            if (step.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    /** How many steps' messages contain the given text. */
    private long countMessagesContaining(List<Step> steps, String text) {
        long count = 0;
        for (Step step : steps) {
            if (step.getMessage().contains(text)) {
                count++;
            }
        }
        return count;
    }

    @Test
    void bellmanFordExample1() {
        List<Step> steps = graphService.bellmanFord(
                bellmanFordExample(), 0);
        assertEquals(List.of(0, 4, 1, 9, 11, 14), lastData(steps));
        // Node 2 is 1 via the negative edge 1-2 (0->1->2) — Dijkstra
        // would settle it at 2 before discovering the shorter path.
        assertEquals(1, lastData(steps).get(2));
        assertTrue(lastStep(steps).getMessage()
                .contains("Shortest distances from 0: [0, 4, 1, 9, 11, 14]"));
    }

    @Test
    void bellmanFordEarlyExit() {
        // Iteration 1 settles the distances; iteration 2 relaxes
        // nothing, so the trace has exactly one "Iteration ...
        // complete" summary step (iteration 1).
        List<Step> steps = graphService.bellmanFord(
                bellmanFordExample(), 0);
        assertEquals(1,
                countMessagesContaining(steps, "complete. Distances:"));
        // The key relaxation that Dijkstra misses is in the trace.
        assertTrue(anyMessageContains(steps,
                "Relax edge 1-2 (weight -3): dist[2] 2 -> 1."));
    }

    @Test
    void bellmanFordNegativeCycle() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1, 1), List.of(1, 2, -2), List.of(2, 0, -1));
        List<Step> steps = graphService.bellmanFord(edges, 0);
        assertTrue(lastStep(steps).getMessage()
                .contains("Negative cycle"));
    }

    @Test
    void topoSortDag() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1), List.of(0, 2), List.of(1, 3),
                List.of(2, 3), List.of(3, 4), List.of(3, 5),
                List.of(4, 5));
        List<Step> steps = graphService.topoSort(edges);
        assertEquals(List.of(0, 1, 2, 3, 4, 5),
                lastStep(steps).getSorted());
        assertTrue(lastStep(steps).getMessage()
                .contains("Topological order: [0, 1, 2, 3, 4, 5]"));
    }

    @Test
    void topoSortCycle() {
        List<List<Integer>> edges = List.of(
                List.of(0, 1), List.of(1, 2), List.of(2, 0));
        List<Step> steps = graphService.topoSort(edges);
        assertTrue(lastStep(steps).getMessage().contains("Cycle"));
    }

    @Test
    void knapsackExample() {
        List<Step> steps = knapsackService.knapsack(
                List.of(2, 3, 4, 5), List.of(3, 4, 5, 6), 5);
        Step last = lastStep(steps);
        assertEquals(7, last.getTable().get(4).get(5));
        assertTrue(last.getMessage().contains("Maximum value: 7"));
        assertTrue(last.getMessage().contains("Items taken: 0, 1"));
        // Full final table (rows 1..4).
        assertEquals(List.of(0, 0, 3, 3, 3, 3), last.getTable().get(1));
        assertEquals(List.of(0, 0, 3, 4, 4, 7), last.getTable().get(2));
        assertEquals(List.of(0, 0, 3, 4, 5, 7), last.getTable().get(3));
        assertEquals(List.of(0, 0, 3, 4, 5, 7), last.getTable().get(4));
        // The step that computes dp[2][5] uses the design doc's message.
        assertTrue(anyMessageContains(steps,
                "dp[2][5] = max(dp[1][5], 4 + dp[1][2]) = max(3, 7) = 7."));
    }

    @Test
    void knapsackSingleItem() {
        List<Step> steps = knapsackService.knapsack(
                List.of(1, 2), List.of(10, 20), 2);
        Step last = lastStep(steps);
        assertEquals(20, last.getTable().get(2).get(2));
        assertTrue(last.getMessage().contains("Maximum value: 20"));
        assertTrue(last.getMessage().contains("Items taken: 1"));
    }

    @Test
    void knapsackCapacityZero() {
        List<Step> steps = knapsackService.knapsack(
                List.of(2, 3, 4, 5), List.of(3, 4, 5, 6), 0);
        Step last = lastStep(steps);
        assertEquals(0, last.getTable().get(4).get(0));
        assertTrue(last.getMessage().contains("Maximum value: 0"));
        assertTrue(last.getMessage().contains("Items taken: none"));
    }
}
