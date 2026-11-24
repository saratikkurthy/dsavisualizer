package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.CentroidService;
import com.sarat.dsavisualizer.service.HldService;
import com.sarat.dsavisualizer.service.HopcroftKarpService;
import com.sarat.dsavisualizer.service.HungarianService;
import com.sarat.dsavisualizer.service.MinCostFlowService;
import com.sarat.dsavisualizer.service.PersistentSegService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 25 service tests. HLD tests check the demo tree
 * (0:[1,2], 1:[3,4], 2:[5], 4:[6,7]: sizes {0:8, 1:5, 2:2,
 * 3:1, 4:3, 5:1, 6:1, 7:1}, heavy 0&rarr;1, 1&rarr;4,
 * 2&rarr;5, 4&rarr;6, positions {0:0, 1:1, 4:2, 6:3, 7:4,
 * 3:5, 2:6, 5:7}, chains [0,1,4,6] / [2,5] / [3] / [7],
 * query 7&rarr;5 in 3 segments, 20 steps) and a 3-node
 * chain. Centroid tests check the demo (picks
 * [1,2,0,5,3,4,6,7], levels {1:0, 2:1, 3:1, 4:1, 0:2, 5:2,
 * 6:2, 7:2}, 9 steps) and a 3-node path. Persistent segment
 * tree tests check the demo array [3,1,4,1,5] (9/12/16
 * nodes, sums 14/19/23, queries 6/11/11, 7 steps) and a
 * no-update run. Hopcroft-Karp tests check the demo
 * (matching {0:4, 1:5, 2:3}, 2 phases, 7 steps — textbook
 * HK finds 0&rarr;3 and 1&rarr;4 in phase 1 and the
 * length-5 path 2&rarr;3&rarr;0&rarr;4&rarr;1&rarr;5 in
 * phase 2; see the service Javadoc) and a 2-edge case.
 * Min-cost max-flow tests check the demo (flow 4, cost 12,
 * 3 steps, the 1&rarr;2 shortcut unused) and a no-path case.
 * Hungarian tests check the demo ([[4,1,3],[2,0,5],[3,2,2]]:
 * row-reduce, col-reduce, 2-line cover (column 1 + row 2),
 * adjust by 1, assignment (0,1),(1,0),(2,2) cost 5,
 * 6 steps) and a 2&times;2 case. The 400 tests call the
 * controller endpoints directly — the controller is built
 * with nulls for the other services because the validators
 * 400 before any service is touched (the Phase 16
 * precedent).
 */
class Phase25ServiceTest {

    private final HldService hldService = new HldService();
    private final CentroidService centroidService =
            new CentroidService();
    private final PersistentSegService persistentSegService =
            new PersistentSegService();
    private final HopcroftKarpService hopcroftKarpService =
            new HopcroftKarpService();
    private final MinCostFlowService minCostFlowService =
            new MinCostFlowService();
    private final HungarianService hungarianService =
            new HungarianService();

    static final List<List<Integer>> TREE_EDGES = List.of(
            List.of(0, 1), List.of(0, 2), List.of(1, 3),
            List.of(1, 4), List.of(2, 5), List.of(4, 6),
            List.of(4, 7));
    static final List<List<Integer>> BIPARTITE_EDGES = List.of(
            List.of(0, 3), List.of(0, 4), List.of(1, 4),
            List.of(1, 5), List.of(2, 3));
    static final List<List<Integer>> FLOW_EDGES = List.of(
            List.of(0, 1, 2), List.of(0, 2, 2), List.of(1, 3, 2),
            List.of(2, 3, 2), List.of(1, 2, 1));
    static final List<Integer> FLOW_COSTS = List.of(1, 3, 1, 1, 1);
    static final String PST_ARRAY = "3,1,4,1,5";
    static final List<Integer> PST_VALUES = List.of(2, 9, 0, 7, 1, 3);
    static final String HUNGARIAN_MATRIX = "4,1,3;2,0,5;3,2,2";

    // ---- HLD ----

    @Test
    void hldDemoStepCount() {
        List<Step> steps = hldService.hld(TREE_EDGES, 7, 5);
        assertEquals(20, steps.size());
    }

    @Test
    void hldDemoSizes() {
        List<Step> steps = hldService.hld(TREE_EDGES, 7, 5);
        // Post-order 3,6,7,4,1,5,2,0; data = [size, heavy or -1].
        assertEquals(List.of(1, -1), steps.get(0).getData());
        assertEquals(List.of(3), steps.get(0).getHighlight());
        assertEquals(List.of(1, -1), steps.get(1).getData());
        assertEquals(List.of(1, -1), steps.get(2).getData());
        assertEquals(List.of(3, 6), steps.get(3).getData());
        assertEquals(List.of(5, 4), steps.get(4).getData());
        assertEquals(List.of(1, -1), steps.get(5).getData());
        assertEquals(List.of(2, 5), steps.get(6).getData());
        assertEquals(List.of(8, 1), steps.get(7).getData());
        // Only node 3 is sized after step 0; all sizes known after step 7.
        assertEquals(1, steps.get(0).getNodes().get(3).getValue());
        assertEquals(0, steps.get(0).getNodes().get(0).getValue());
        assertEquals(8, steps.get(7).getNodes().get(0).getValue());
        assertEquals(5, steps.get(7).getNodes().get(1).getValue());
        assertEquals(3, steps.get(7).getNodes().get(4).getValue());
        assertEquals(2, steps.get(7).getLine());
    }

    @Test
    void hldDemoDecompose() {
        List<Step> steps = hldService.hld(TREE_EDGES, 7, 5);
        // Decompose order 0,1,4,6,7,3,2,5; data = [pos, head].
        assertEquals(List.of(0, 0), steps.get(8).getData());
        assertEquals(List.of(1, 0), steps.get(9).getData());
        assertEquals(List.of(2, 0), steps.get(10).getData());
        assertEquals(List.of(3, 0), steps.get(11).getData());
        assertEquals(List.of(4, 7), steps.get(12).getData());
        assertEquals(List.of(5, 3), steps.get(13).getData());
        assertEquals(List.of(6, 2), steps.get(14).getData());
        assertEquals(List.of(7, 2), steps.get(15).getData());
        // Node 7 is the first head that is not 0; node 3 still reads 0 at step 12.
        assertEquals(7, steps.get(12).getNodes().get(7).getValue());
        assertEquals(0, steps.get(12).getNodes().get(3).getValue());
        assertEquals(3, steps.get(8).getLine());
    }

    @Test
    void hldDemoQuery() {
        List<Step> steps = hldService.hld(TREE_EDGES, 7, 5);
        List<Integer> path = List.of(7, 4, 1, 0, 2, 5);
        assertEquals(List.of(7), steps.get(16).getHighlight());
        assertEquals(List.of(0), steps.get(16).getData());
        assertEquals(path, steps.get(16).getSorted());
        assertEquals(List.of(0, 1, 4), steps.get(17).getHighlight());
        assertEquals(List.of(1), steps.get(17).getData());
        assertEquals(path, steps.get(17).getSorted());
        assertEquals(List.of(2, 5), steps.get(18).getHighlight());
        assertEquals(List.of(2), steps.get(18).getData());
        assertEquals(4, steps.get(16).getLine());
        // Final step: 4 chains.
        assertEquals(List.of(4), steps.get(19).getData());
        assertEquals(path, steps.get(19).getSorted());
        assertEquals(0, steps.get(19).getLine());
        assertEquals(7, steps.get(19).getEdges().size());
    }

    @Test
    void hldChainTree() {
        List<Step> steps = hldService.hld(
                List.of(List.of(0, 1), List.of(1, 2)), 2, 0);
        // 3 size + 3 decompose + 1 query + final = 8 steps, 1 chain.
        assertEquals(8, steps.size());
        assertEquals(List.of(3, 1), steps.get(2).getData());
        assertEquals(List.of(0, 1, 2), steps.get(6).getHighlight());
        assertEquals(List.of(1), steps.get(7).getData());
    }

    // ---- Centroid ----

    @Test
    void centroidDemoStepCount() {
        List<Step> steps = centroidService.centroid(TREE_EDGES);
        assertEquals(9, steps.size());
    }

    @Test
    void centroidDemoPickOrder() {
        List<Step> steps = centroidService.centroid(TREE_EDGES);
        List<Integer> picks = List.of(1, 2, 0, 5, 3, 4, 6, 7);
        // data = [level, parent or -1]; sorted grows one pick at a time.
        assertEquals(List.of(0, -1), steps.get(0).getData());
        assertEquals(List.of(1, 1), steps.get(1).getData());
        assertEquals(List.of(2, 2), steps.get(2).getData());
        assertEquals(List.of(2, 2), steps.get(3).getData());
        assertEquals(List.of(1, 1), steps.get(4).getData());
        assertEquals(List.of(1, 1), steps.get(5).getData());
        assertEquals(List.of(2, 4), steps.get(6).getData());
        assertEquals(List.of(2, 4), steps.get(7).getData());
        for (int i = 0; i < 8; i++) {
            assertEquals(picks.subList(0, i + 1),
                    steps.get(i).getSorted());
            assertEquals(List.of(picks.get(i)),
                    steps.get(i).getHighlight());
            assertEquals(2, steps.get(i).getLine());
        }
        assertEquals(List.of(2), steps.get(8).getData());
        assertEquals(picks, steps.get(8).getSorted());
        assertEquals(0, steps.get(8).getLine());
    }

    @Test
    void centroidDemoLevelsAndEdges() {
        List<Step> steps = centroidService.centroid(TREE_EDGES);
        // Final nodes: value = level. Final edges: the centroid tree.
        assertEquals(0, steps.get(8).getNodes().get(1).getValue());
        assertEquals(1, steps.get(8).getNodes().get(2).getValue());
        assertEquals(1, steps.get(8).getNodes().get(3).getValue());
        assertEquals(1, steps.get(8).getNodes().get(4).getValue());
        assertEquals(2, steps.get(8).getNodes().get(0).getValue());
        assertEquals(2, steps.get(8).getNodes().get(5).getValue());
        assertEquals(2, steps.get(8).getNodes().get(6).getValue());
        assertEquals(2, steps.get(8).getNodes().get(7).getValue());
        assertEquals(7, steps.get(8).getEdges().size());
        assertEquals(1, steps.get(8).getEdges().get(0).getFrom());
        assertEquals(2, steps.get(8).getEdges().get(0).getTo());
        assertEquals(4, steps.get(8).getEdges().get(6).getFrom());
        assertEquals(7, steps.get(8).getEdges().get(6).getTo());
        // Before pick 4, node 4 still reads -1.
        assertEquals(-1, steps.get(4).getNodes().get(4).getValue());
        assertEquals(5, steps.get(5).getEdges().size());
    }

    @Test
    void centroidPathThree() {
        List<Step> steps = centroidService.centroid(
                List.of(List.of(0, 1), List.of(1, 2)));
        assertEquals(4, steps.size());
        assertEquals(List.of(1, 0, 2), steps.get(3).getSorted());
        assertEquals(List.of(1), steps.get(3).getData());
    }

    // ---- Persistent segment tree ----

    @Test
    void persistentSegDemoStepCount() {
        List<Step> steps = persistentSegService.persistentSeg(
                PST_ARRAY, PST_VALUES);
        assertEquals(7, steps.size());
    }

    @Test
    void persistentSegDemoBuild() {
        List<Step> steps = persistentSegService.persistentSeg(
                PST_ARRAY, PST_VALUES);
        assertEquals(List.of(9), steps.get(0).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                steps.get(0).getSorted());
        assertEquals(1, steps.get(0).getLine());
        assertEquals(9, steps.get(0).getNodes().size());
        assertEquals(8, steps.get(0).getEdges().size());
        // Preorder numbering: root [0,4] sum 14, [0,2] sum 8, [0,1] sum 4.
        assertEquals(14, steps.get(0).getNodes().get(0).getValue());
        assertEquals("[0,4]", steps.get(0).getNodes().get(0).getLabel());
        assertEquals(8, steps.get(0).getNodes().get(1).getValue());
        assertEquals("[0,2]", steps.get(0).getNodes().get(1).getLabel());
        assertEquals(4, steps.get(0).getNodes().get(2).getValue());
        assertEquals(3, steps.get(0).getNodes().get(3).getValue());
        assertEquals(1, steps.get(0).getNodes().get(4).getValue());
        assertEquals(4, steps.get(0).getNodes().get(5).getValue());
        assertEquals(6, steps.get(0).getNodes().get(6).getValue());
        assertEquals(1, steps.get(0).getNodes().get(7).getValue());
        assertEquals(5, steps.get(0).getNodes().get(8).getValue());
    }

    @Test
    void persistentSegDemoUpdates() {
        List<Step> steps = persistentSegService.persistentSeg(
                PST_ARRAY, PST_VALUES);
        assertEquals(List.of(12), steps.get(1).getData());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals(List.of(2, 3, 4, 6, 7, 8, 9, 10, 11),
                steps.get(1).getSorted());
        assertEquals(19, steps.get(1).getNodes().get(9).getValue());
        assertEquals(12, steps.get(1).getNodes().size());
        assertEquals(12, steps.get(1).getEdges().size());
        // v0 is unchanged: node 0 still sums 14.
        assertEquals(14, steps.get(1).getNodes().get(0).getValue());
        assertEquals(List.of(16), steps.get(2).getData());
        assertEquals(List.of(0), steps.get(2).getHighlight());
        assertEquals(List.of(4, 6, 7, 8, 11, 12, 13, 14, 15),
                steps.get(2).getSorted());
        assertEquals(23, steps.get(2).getNodes().get(12).getValue());
        assertEquals(16, steps.get(2).getNodes().size());
        assertEquals(18, steps.get(2).getEdges().size());
        assertEquals(19, steps.get(2).getNodes().get(9).getValue());
        assertEquals(2, steps.get(1).getLine());
    }

    @Test
    void persistentSegDemoQueries() {
        List<Step> steps = persistentSegService.persistentSeg(
                PST_ARRAY, PST_VALUES);
        assertEquals(List.of(6), steps.get(3).getData());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
                steps.get(3).getSorted());
        assertEquals(List.of(11), steps.get(4).getData());
        assertEquals(List.of(2, 3, 4, 6, 7, 8, 9, 10, 11),
                steps.get(4).getSorted());
        assertEquals(List.of(11), steps.get(5).getData());
        assertEquals(List.of(4, 6, 7, 8, 11, 12, 13, 14, 15),
                steps.get(5).getSorted());
        assertEquals(List.of(1, 2, 3), steps.get(3).getHighlight());
        assertEquals(3, steps.get(3).getLine());
        assertEquals(List.of(16), steps.get(6).getData());
        assertEquals(List.of(4, 6, 7, 8, 11, 12, 13, 14, 15),
                steps.get(6).getSorted());
        assertEquals(0, steps.get(6).getLine());
    }

    @Test
    void persistentSegNoUpdate() {
        List<Step> steps = persistentSegService.persistentSeg(
                "5,5", List.of(0, 1));
        // Build + 1 query + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals(List.of(3), steps.get(0).getData());
        assertEquals(List.of(10), steps.get(1).getData());
        assertEquals(List.of(3), steps.get(2).getData());
    }

    // ---- Hopcroft-Karp ----

    @Test
    void hopcroftKarpDemoStepCount() {
        List<Step> steps = hopcroftKarpService.hopcroftKarp(
                BIPARTITE_EDGES);
        assertEquals(7, steps.size());
    }

    @Test
    void hopcroftKarpDemoMatching() {
        List<Step> steps = hopcroftKarpService.hopcroftKarp(
                BIPARTITE_EDGES);
        assertEquals(List.of(3), steps.get(6).getData());
        assertEquals(List.of(0, 4, 1, 5, 2, 3),
                steps.get(6).getSorted());
        assertEquals(0, steps.get(6).getLine());
        // Final nodes: value = partner or -1; labels L1..L3 / R1..R3.
        assertEquals(4, steps.get(6).getNodes().get(0).getValue());
        assertEquals("L1", steps.get(6).getNodes().get(0).getLabel());
        assertEquals(5, steps.get(6).getNodes().get(1).getValue());
        assertEquals(3, steps.get(6).getNodes().get(2).getValue());
        assertEquals(2, steps.get(6).getNodes().get(3).getValue());
        assertEquals("R1", steps.get(6).getNodes().get(3).getLabel());
        assertEquals(0, steps.get(6).getNodes().get(4).getValue());
        assertEquals(1, steps.get(6).getNodes().get(5).getValue());
        assertEquals(5, steps.get(6).getEdges().size());
    }

    @Test
    void hopcroftKarpDemoPhases() {
        List<Step> steps = hopcroftKarpService.hopcroftKarp(
                BIPARTITE_EDGES);
        // Phase 1 BFS: free left [0,1,2], size 0.
        assertEquals(List.of(0, 1, 2), steps.get(0).getHighlight());
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(), steps.get(0).getSorted());
        assertEquals(2, steps.get(0).getLine());
        // Textbook HK finds 0->3 and 1->4 in phase 1.
        assertEquals(List.of(0, 3), steps.get(1).getHighlight());
        assertEquals(List.of(1), steps.get(1).getData());
        assertEquals(List.of(0, 3), steps.get(1).getSorted());
        assertEquals(List.of(1, 4), steps.get(2).getHighlight());
        assertEquals(List.of(2), steps.get(2).getData());
        assertEquals(List.of(0, 3, 1, 4), steps.get(2).getSorted());
        assertEquals(3, steps.get(1).getLine());
        // Phase 2 BFS: only vertex 2 is free; then the length-5 path.
        assertEquals(List.of(2), steps.get(3).getHighlight());
        assertEquals(List.of(2), steps.get(3).getData());
        assertEquals(List.of(2, 3, 0, 4, 1, 5),
                steps.get(4).getHighlight());
        assertEquals(List.of(3), steps.get(4).getData());
        assertEquals(List.of(0, 4, 1, 5, 2, 3),
                steps.get(4).getSorted());
        // Final BFS: nothing left to augment.
        assertEquals(List.of(), steps.get(5).getHighlight());
        assertEquals(List.of(3), steps.get(5).getData());
    }

    @Test
    void hopcroftKarpTwoEdges() {
        List<Step> steps = hopcroftKarpService.hopcroftKarp(
                List.of(List.of(0, 3), List.of(1, 4)));
        // BFS + 2 DFS + final BFS + final = 5 steps, matching size 2.
        assertEquals(5, steps.size());
        assertEquals(List.of(2), steps.get(4).getData());
        assertEquals(List.of(0, 3, 1, 4), steps.get(4).getSorted());
    }

    // ---- Min-cost max-flow ----

    @Test
    void minCostFlowDemoStepCount() {
        List<Step> steps = minCostFlowService.minCostFlow(
                FLOW_EDGES, FLOW_COSTS, 0, 3);
        assertEquals(3, steps.size());
    }

    @Test
    void minCostFlowDemoFlowAndCost() {
        List<Step> steps = minCostFlowService.minCostFlow(
                FLOW_EDGES, FLOW_COSTS, 0, 3);
        assertEquals(List.of(2, 4), steps.get(0).getData());
        assertEquals(List.of(0, 1, 3), steps.get(0).getHighlight());
        assertEquals(List.of(2, 0, 2, 0, 0), steps.get(0).getSorted());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(4, 12), steps.get(1).getData());
        assertEquals(List.of(0, 2, 3), steps.get(1).getHighlight());
        assertEquals(List.of(2, 2, 2, 2, 0), steps.get(1).getSorted());
        assertEquals(List.of(4, 12), steps.get(2).getData());
        assertEquals(List.of(2, 2, 2, 2, 0), steps.get(2).getSorted());
        assertEquals(0, steps.get(2).getLine());
        // Node labels s/a/b/t; edge weights = the flows.
        assertEquals("s", steps.get(2).getNodes().get(0).getLabel());
        assertEquals("t", steps.get(2).getNodes().get(3).getLabel());
        assertEquals(2, steps.get(2).getEdges().get(0).getWeight());
        assertEquals(2, steps.get(2).getEdges().get(3).getWeight());
        assertEquals(0, steps.get(2).getEdges().get(4).getWeight());
    }

    @Test
    void minCostFlowShortcutUnused() {
        List<Step> steps = minCostFlowService.minCostFlow(
                FLOW_EDGES, FLOW_COSTS, 0, 3);
        // The 1->2 shortcut (index 4) never carries flow.
        assertEquals(0, steps.get(0).getSorted().get(4));
        assertEquals(0, steps.get(1).getSorted().get(4));
        assertEquals(0, steps.get(2).getSorted().get(4));
    }

    @Test
    void minCostFlowNoPath() {
        List<Step> steps = minCostFlowService.minCostFlow(
                List.of(List.of(0, 1, 1), List.of(2, 3, 1)),
                List.of(1, 1), 0, 3);
        // No s->t path: just the final step, flow 0 cost 0.
        assertEquals(1, steps.size());
        assertEquals(List.of(0, 0), steps.get(0).getData());
    }

    // ---- Hungarian ----

    @Test
    void hungarianDemoStepCount() {
        List<Step> steps = hungarianService.hungarian(
                HUNGARIAN_MATRIX);
        assertEquals(6, steps.size());
    }

    @Test
    void hungarianDemoReductions() {
        List<Step> steps = hungarianService.hungarian(
                HUNGARIAN_MATRIX);
        assertEquals(List.of(3, 0, 2, 2, 0, 5, 1, 0, 0),
                steps.get(0).getData());
        assertEquals(List.of(0, 1, 2, 6, 7, 8),
                steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(2, 0, 2, 1, 0, 5, 0, 0, 0),
                steps.get(1).getData());
        assertEquals(List.of(0, 3, 6), steps.get(1).getHighlight());
        assertEquals(3, steps.get(1).getLine());
        // Node 0 is cell (0,0): value 3 after row reduce, 2 after col reduce.
        assertEquals(3, steps.get(0).getNodes().get(0).getValue());
        assertEquals("3", steps.get(0).getNodes().get(0).getLabel());
        assertEquals(2, steps.get(1).getNodes().get(0).getValue());
        assertEquals("2", steps.get(1).getNodes().get(0).getLabel());
        // Node 4 is cell (1,1): value 0 throughout.
        assertEquals(0, steps.get(0).getNodes().get(4).getValue());
        assertEquals("0", steps.get(0).getNodes().get(4).getLabel());
    }

    @Test
    void hungarianDemoCoverAndAdjust() {
        List<Step> steps = hungarianService.hungarian(
                HUNGARIAN_MATRIX);
        // Cover: column 1 + row 2 = cells 1,4,6,7,8.
        assertEquals(List.of(2, 0, 2, 1, 0, 5, 0, 0, 0),
                steps.get(2).getData());
        assertEquals(List.of(1, 4, 6, 7, 8),
                steps.get(2).getHighlight());
        assertEquals(4, steps.get(2).getLine());
        // Adjust by 1: [[1,0,1],[0,0,4],[0,1,0]].
        assertEquals(List.of(1, 0, 1, 0, 0, 4, 0, 1, 0),
                steps.get(3).getData());
        assertEquals(List.of(0, 2, 3, 5, 7),
                steps.get(3).getHighlight());
        assertEquals(5, steps.get(3).getLine());
    }

    @Test
    void hungarianDemoAssignment() {
        List<Step> steps = hungarianService.hungarian(
                HUNGARIAN_MATRIX);
        assertEquals(List.of(0, 1, 1, 0, 2, 2),
                steps.get(4).getSorted());
        assertEquals(List.of(1, 3, 8), steps.get(4).getHighlight());
        assertEquals(6, steps.get(4).getLine());
        assertEquals(List.of(1, 0, 1, 0, 0, 4, 0, 1, 0),
                steps.get(5).getData());
        assertEquals(List.of(0, 1, 1, 0, 2, 2),
                steps.get(5).getSorted());
        assertEquals(0, steps.get(5).getLine());
        // Cost from the ORIGINAL matrix + sorted: 1+2+2 = 5.
        int[][] original = {{4, 1, 3}, {2, 0, 5}, {3, 2, 2}};
        List<Integer> sorted = steps.get(5).getSorted();
        int cost = 0;
        for (int i = 0; i < sorted.size(); i += 2) {
            cost += original[sorted.get(i)][sorted.get(i + 1)];
        }
        assertEquals(5, cost);
        assertEquals(9, steps.get(5).getNodes().size());
        assertTrue(steps.get(5).getEdges() == null);
    }

    @Test
    void hungarianTwoByTwo() {
        List<Step> steps = hungarianService.hungarian("1,2;3,4");
        // Row-reduce, col-reduce, assign, final = 4 steps (no adjust needed).
        assertEquals(4, steps.size());
        assertEquals(List.of(0, 0, 0, 0), steps.get(1).getData());
        assertEquals(List.of(0, 1, 1, 0), steps.get(2).getSorted());
        int[][] original = {{1, 2}, {3, 4}};
        List<Integer> sorted = steps.get(2).getSorted();
        int cost = original[sorted.get(0)][sorted.get(1)]
                + original[sorted.get(2)][sorted.get(3)];
        assertEquals(5, cost);
    }

    // ---- 400s (controller validators) ----

    /** The controller under test: nulls for every service except
     * the Phase 25 ones (validation 400s before any service is
     * called). The parameter order is the constructor's: 57
     * Phase 1–24 params, then hld, centroid, persistentseg,
     * hopcroftkarp, mincostflow, hungarian (63 total). */
    
    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    /** A request with only graphEdges set. */
    private TraceRequest edgesRequest(List<List<Integer>> edges) {
        TraceRequest request = new TraceRequest();
        request.setGraphEdges(edges);
        return request;
    }

    /** A request with only the word set. */
    private TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

}
