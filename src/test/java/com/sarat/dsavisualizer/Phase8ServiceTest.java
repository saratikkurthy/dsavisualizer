package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.GraphService;
import com.sarat.dsavisualizer.service.ShellSortService;
import com.sarat.dsavisualizer.service.TrieService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 8 service tests. MST tests check the final step's total
 * weight and mstEdges; trie tests check the final step's nodes,
 * edges and word ends; shell sort tests check the last step's data.
 */
class Phase8ServiceTest {

    private final GraphService graphService = new GraphService();
    private final TrieService trieService = new TrieService();
    private final ShellSortService shellSortService = new ShellSortService();

    /** The weighted example shared by the Prim's/Kruskal's tests. */
    private List<List<Integer>> weightedExample() {
        return List.of(
                List.of(0, 1, 4), List.of(0, 2, 2), List.of(1, 2, 1),
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

    /**
     * Edges as unordered pairs (min, max), in order — so an edge
     * written [2, 1] and one written [1, 2] compare equal.
     */
    private List<List<Integer>> normalizedEdges(List<List<Integer>> edges) {
        List<List<Integer>> norm = new ArrayList<>();
        for (List<Integer> e : edges) {
            norm.add(List.of(Math.min(e.get(0), e.get(1)),
                    Math.max(e.get(0), e.get(1))));
        }
        return norm;
    }

    /**
     * Follows a path of labels from the root node (label "") and
     * returns the end node id. Throws if a character is missing.
     */
    private int followPath(Step step, String path) {
        Map<Integer, String> labelById = new HashMap<>();
        for (NodeDto node : step.getNodes()) {
            labelById.put(node.getId(), node.getLabel());
        }
        Map<Integer, Map<String, Integer>> children = new HashMap<>();
        for (EdgeDto edge : step.getEdges()) {
            children.computeIfAbsent(edge.getFrom(), k -> new HashMap<>())
                    .put(labelById.get(edge.getTo()), edge.getTo());
        }
        int rootId = -1;
        for (NodeDto node : step.getNodes()) {
            if ("".equals(node.getLabel())) {
                rootId = node.getId();
            }
        }
        int current = rootId;
        for (int i = 0; i < path.length(); i++) {
            String ch = String.valueOf(path.charAt(i));
            Map<String, Integer> kids = children.get(current);
            if (kids == null || !kids.containsKey(ch)) {
                throw new AssertionError("No '" + ch + "' after '"
                        + path.substring(0, i) + "'");
            }
            current = kids.get(ch);
        }
        return current;
    }

    @Test
    void primWeightedExample() {
        // Add 2 (key 2, edge 0-2), add 1 (key 1, edge 2-1),
        // add 3 (key 5, edge 1-3), add 4 (key 2, edge 3-4),
        // add 5 (key 3, edge 4-5). Total 2+1+5+2+3 = 13.
        List<Step> steps = graphService.prim(weightedExample(), 0);
        Step last = lastStep(steps);
        assertTrue(last.getMessage().contains("MST total weight: 13."),
                "Last message was: " + last.getMessage());
        assertEquals(List.of(
                List.of(0, 2), List.of(2, 1), List.of(1, 3),
                List.of(3, 4), List.of(4, 5)), last.getMstEdges());
    }

    @Test
    void primKeysAreEdgeWeightsNotPathDistances() {
        // The CRITICAL Prim's vs Dijkstra distinction: key[v] is the
        // minimum EDGE weight connecting v to the MST, not the
        // shortest PATH distance from the start. Dijkstra on this
        // graph gives [0, 3, 2, 8, 10, 13]; Prim's keys are
        // [0, 1, 2, 5, 2, 3] (edge 2-1 is weight 1, edge 1-3 is
        // weight 5, edge 3-4 is weight 2, edge 4-5 is weight 3).
        List<Step> steps = graphService.prim(weightedExample(), 0);
        assertEquals(List.of(0, 1, 2, 5, 2, 3), lastData(steps));
        assertTrue(anyMessageContains(steps,
                "Relax edge 1-3: key 8 -> 5."),
                "Expected the 1-3 relax step in the trace");
        assertTrue(anyMessageContains(steps,
                "Add node 3 to the MST via edge 1-3 (weight 5). "
                        + "Total weight: 8."),
                "Expected the node-3 add step in the trace");
    }

    @Test
    void kruskalWeightedExample() {
        // Sorted edges: (2-1,1), (0-2,2), (3-4,2), (4-5,3),
        // (0-1,4), (1-3,5), (3-5,6), (2-3,8), (2-4,10).
        // 0-1 is skipped (0 and 1 are already connected via
        // 0-2-1); stop after 1-3 (5 edges). Total 1+2+2+3+5 = 13.
        // NOTE: the design doc lists the first edge as "2-1"; the
        // input edge is [1, 2, 1], so the service stores [1, 2] —
        // the same undirected edge, compared here as unordered pairs.
        List<Step> steps = graphService.kruskal(weightedExample());
        Step last = lastStep(steps);
        assertTrue(last.getMessage().contains("MST total weight: 13."),
                "Last message was: " + last.getMessage());
        assertEquals(List.of(
                List.of(1, 2), List.of(0, 2), List.of(3, 4),
                List.of(4, 5), List.of(1, 3)),
                normalizedEdges(last.getMstEdges()));
        assertTrue(anyMessageContains(steps,
                "Skip edge 0-1 (weight 4) — it would form a cycle."),
                "Expected the 0-1 skip step in the trace");
    }

    @Test
    void trieInsertAddsDogPath() {
        // Build from cat + car, then insert dog: new nodes d, o, g
        // under the root, with g a word end.
        List<Step> steps = trieService.insert(
                List.of("cat", "car"), "dog");
        Step last = lastStep(steps);
        int gId = followPath(last, "dog");
        assertTrue(last.getSorted().contains(gId),
                "g should be a word end, but sorted was "
                        + last.getSorted());
        assertTrue(last.getMessage().contains("Inserted 'dog'."),
                "Last message was: " + last.getMessage());
    }

    @Test
    void trieSearchFound() {
        List<Step> steps = trieService.search(
                List.of("cat", "car"), "car");
        assertTrue(lastStep(steps).getMessage().contains("Found"),
                "Last message was: " + lastStep(steps).getMessage());
    }

    @Test
    void trieSearchPrefixButNotWord() {
        // "ca" is a path in the trie but no word ends at 'a'.
        List<Step> steps = trieService.search(
                List.of("cat", "car"), "ca");
        assertTrue(lastStep(steps).getMessage()
                        .contains("prefix but not a word"),
                "Last message was: " + lastStep(steps).getMessage());
    }

    @Test
    void trieSearchNotFound() {
        // No 'd' under the root at all.
        List<Step> steps = trieService.search(
                List.of("cat", "car"), "dog");
        assertTrue(lastStep(steps).getMessage().contains("Not found"),
                "Last message was: " + lastStep(steps).getMessage());
    }

    @Test
    void shellSortClassicExample() {
        // Gap 3: [1, 2, 3, 5, 6, 8]; gap 1: already sorted.
        // NOTE: the design doc's draft said the first gap-3 step
        // was "Gap 3: compare 5 and 2.", but at gap 3 i=3 compares
        // a[0]=5 with a[3]=1 — 5 and 2 are never gap-3 apart
        // (indexes 0 and 1). "Gap 3: compare 5 and 1." is correct.
        List<Step> steps = shellSortService.shellSort(
                List.of(5, 2, 8, 1, 6, 3));
        assertEquals(List.of(1, 2, 3, 5, 6, 8), lastData(steps));
        assertTrue(anyMessageContains(steps, "Gap 3: compare 5 and 1."),
                "Expected the gap-3 compare step in the trace");
        assertTrue(anyMessageContains(steps, "Gap 3: swap 5 and 1."),
                "Expected the gap-3 swap step in the trace");
    }

    @Test
    void shellSortSmall() {
        List<Step> steps = shellSortService.shellSort(List.of(3, 1, 2));
        assertEquals(List.of(1, 2, 3), lastData(steps));
    }

    @Test
    void shellSortSingleElement() {
        List<Step> steps = shellSortService.shellSort(List.of(5));
        assertEquals(List.of(5), lastData(steps));
    }
}
