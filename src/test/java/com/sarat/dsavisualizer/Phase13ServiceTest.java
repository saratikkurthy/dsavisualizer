package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.BacktrackingService;
import com.sarat.dsavisualizer.service.KmpService;
import com.sarat.dsavisualizer.service.SegmentTreeService;
import com.sarat.dsavisualizer.service.UnionFindService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 13 service tests. Segment-tree tests check the whole tree
 * array (index 0 unused, kept as 0 so positions match tree
 * indices); union-find tests check the parent array right after
 * the unions and the final sets message; KMP tests check the LPS
 * array and the match position; N-Queens tests check the first
 * solution's columns and the total solution count.
 *
 * All values were verified by Python simulation before this test
 * was written. One subtlety the simulation confirmed: the
 * union-find parent array is the state right after the unions —
 * a later find(5) would compress parent[5] to 0 as well, so the
 * service snapshots the array before computing the final sets.
 */
class Phase13ServiceTest {

    private final SegmentTreeService segmentTreeService =
            new SegmentTreeService();
    private final UnionFindService unionFindService =
            new UnionFindService();
    private final KmpService kmpService = new KmpService();
    private final BacktrackingService backtrackingService =
            new BacktrackingService();

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
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

    // ---- Segment-tree tests ----

    @Test
    void segmentTreeBuild() {
        // Leaves 1,3,5,7,9,11 at tree[6..11]; internal nodes
        // bottom-up: tree[5]=20, tree[4]=12, tree[3]=4,
        // tree[2]=32, tree[1]=36.
        List<Step> steps = segmentTreeService.build(
                List.of(1, 3, 5, 7, 9, 11));
        Step last = lastStep(steps);
        assertEquals(
                List.of(0, 36, 32, 4, 12, 20, 1, 3, 5, 7, 9, 11),
                last.getData());
        assertTrue(last.getMessage()
                .contains("Segment tree built. Total sum 36."));
        // The drawing carries one node per tree index (1..11);
        // the root node (id 1) holds the total.
        assertEquals(11, last.getNodes().size());
        Map<Integer, NodeDto> nodes = new HashMap<>();
        for (NodeDto node : last.getNodes()) {
            nodes.put(node.getId(), node);
        }
        assertEquals(36, nodes.get(1).getValue());
        assertEquals(10, last.getEdges().size());
    }

    @Test
    void segmentTreeQuery() {
        // query(1,4): visit tree[7]=3, tree[10]=9, tree[4]=12 —
        // 3 + 9 + 12 = 24. query(0,5): the whole array, 36.
        List<Step> steps = segmentTreeService.query(
                List.of(1, 3, 5, 7, 9, 11), 1, 4);
        assertTrue(lastStep(steps).getMessage()
                .contains("Range sum [1, 4] = 24."));
        List<Step> full = segmentTreeService.query(
                List.of(1, 3, 5, 7, 9, 11), 0, 5);
        assertTrue(lastStep(full).getMessage()
                .contains("Range sum [0, 5] = 36."));
    }

    @Test
    void segmentTreeUpdate() {
        // Update index 2 (leaf tree[8]) from 5 to 10: ancestors
        // tree[4] = 17, tree[2] = 37, tree[1] = 41.
        List<Step> steps = segmentTreeService.update(
                List.of(1, 3, 5, 7, 9, 11), 2, 10);
        Step last = lastStep(steps);
        assertEquals(41, last.getData().get(1));
        assertEquals(10, last.getData().get(8));
        assertTrue(last.getMessage()
                .contains("Updated position 2 to 10. New total 41."));
        // Query(2,2) on the updated array reads the new leaf.
        List<Step> query = segmentTreeService.query(
                List.of(1, 3, 10, 7, 9, 11), 2, 2);
        assertTrue(lastStep(query).getMessage()
                .contains("Range sum [2, 2] = 10."));
    }

    // ---- Union-find tests ----

    @Test
    void unionFindSequenceOne() {
        // union(0,1): attach 1 under 0. union(1,2): find(1)=0,
        // attach 2 under 0. union(3,4): attach 4 under 3.
        List<Step> steps = unionFindService.union(
                List.of(0, 1, 2, 3, 4, 5),
                List.of(List.of(0, 1), List.of(1, 2),
                        List.of(3, 4)));
        assertEquals(List.of(0, 0, 0, 3, 3, 5),
                lastStep(steps).getData());
        assertTrue(lastStep(steps).getMessage().contains(
                "3 sets: {0, 1, 2}, {3, 4}, {5}."));
    }

    @Test
    void unionFindSequenceTwo() {
        // After [0,1],[2,3],[4,5]: parent [0,0,2,2,4,4].
        // union(1,3): roots 0 and 2, equal ranks — attach 2
        // under 0, rank[0] becomes 2. union(3,5): find(3) walks
        // 3→2→0 and path compression rewires parent[3] to 0;
        // find(5)=4, rank[0]=2 > rank[4]=1, attach 4 under 0.
        List<Step> steps = unionFindService.union(
                List.of(0, 1, 2, 3, 4, 5),
                List.of(List.of(0, 1), List.of(2, 3),
                        List.of(4, 5), List.of(1, 3),
                        List.of(3, 5)));
        assertEquals(List.of(0, 0, 0, 0, 0, 4),
                lastStep(steps).getData());
        assertTrue(lastStep(steps).getMessage().contains(
                "1 set: {0, 1, 2, 3, 4, 5}."));
        assertTrue(anyMessageContains(steps,
                "Path compression: parent[3] = 0."));
    }

    // ---- KMP tests ----

    @Test
    void kmpMatch() {
        // Classic example: LPS [0,0,1,2,0,1,2,3,4], the pattern
        // ABABCABAB first occurs at text index 10.
        List<Step> steps = kmpService.kmp(
                List.of("ABABDABACDABABCABAB", "ABABCABAB"));
        Step last = lastStep(steps);
        assertEquals(List.of(0, 0, 1, 2, 0, 1, 2, 3, 4),
                last.getData());
        assertTrue(last.getMessage()
                .contains("Pattern found at index 10."));
    }

    @Test
    void kmpNoMatch() {
        // AAAA vs AAAB: LPS [0,1,2,0], the pattern never matches
        // (the text runs out while j=3, one character short).
        List<Step> steps = kmpService.kmp(List.of("AAAA", "AAAB"));
        Step last = lastStep(steps);
        assertEquals(List.of(0, 1, 2, 0), last.getData());
        assertTrue(last.getMessage().contains("Pattern not found."));
    }

    // ---- N-Queens tests ----

    /** The column of the queen in each row of the final board. */
    private List<Integer> solutionColumns(Step last) {
        List<Integer> cols = new ArrayList<>();
        for (List<Integer> row : last.getTable()) {
            cols.add(row.indexOf(1));
        }
        return cols;
    }

    @Test
    void nQueensFour() {
        // Rows in order, columns ascending: row 0 col 1, row 1
        // col 3, row 2 col 0, row 3 col 2. Two solutions in total.
        List<Step> steps = backtrackingService.nQueens(4);
        Step last = lastStep(steps);
        assertEquals(List.of(1, 3, 0, 2), solutionColumns(last));
        assertTrue(last.getMessage()
                .contains("Solution 1 of 2 for n = 4."));
    }

    @Test
    void nQueensEight() {
        // 92 solutions in total; the trace shows the first one.
        List<Step> steps = backtrackingService.nQueens(8);
        Step last = lastStep(steps);
        assertTrue(last.getMessage()
                .contains("Solution 1 of 92 for n = 8."));
        // All 8 rows hold exactly one queen, in 8 distinct columns.
        List<Integer> cols = solutionColumns(last);
        assertEquals(8, cols.size());
        assertTrue(cols.stream().allMatch(c -> c >= 0));
        assertEquals(8, new HashSet<>(cols).size());
    }

    @Test
    void nQueensTwo() {
        // No solution exists for n = 2 — the trace is a single step.
        List<Step> steps = backtrackingService.nQueens(2);
        assertEquals(1, steps.size());
        assertTrue(lastStep(steps).getMessage()
                .contains("No solution exists for n = 2."));
    }
}
