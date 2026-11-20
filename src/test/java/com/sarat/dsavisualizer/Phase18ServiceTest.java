package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.EdgeDto;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.BTreeService;
import com.sarat.dsavisualizer.service.BloomFilterService;
import com.sarat.dsavisualizer.service.LruCacheService;
import com.sarat.dsavisualizer.service.SkipListService;
import com.sarat.dsavisualizer.service.SuffixArrayService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 18 service tests. B-tree search tests check the found /
 * not-found / final messages verbatim and the visited-node path
 * (via highlights) for the verified demo tree
 * ([10, 20] | [5, 6, 7] [12, 17] [30]). B-tree delete tests
 * check every phase message verbatim and the final tree for all
 * six verified cases (leaf, predecessor, borrow, merge, small
 * borrow, not present). Suffix-array tests check the SA and LCP
 * for "banana" and the single-character edge case. Skip-list
 * tests pin the Random(42) levels, tower heights, cell/edge
 * counts and the final messages. The 400 tests call the
 * controller endpoints directly — the controller is built with
 * nulls for the other services because the validators 400
 * before any service is touched (the Phase 16 precedent).
 */
class Phase18ServiceTest {

    private final LruCacheService lruCacheService = new LruCacheService();
    private final BTreeService bTreeService = new BTreeService();
    private final BloomFilterService bloomFilterService =
            new BloomFilterService();
    private final SuffixArrayService suffixArrayService =
            new SuffixArrayService();
    private final SkipListService skipListService = new SkipListService();

    /** The demo tree values (Phase 17's final tree). */
    private static final List<Integer> DEMO_TREE =
            List.of(10, 20, 5, 6, 12, 30, 7, 17);

    /** Final step of a trace. */
    private Step lastStep(List<Step> steps) {
        return steps.get(steps.size() - 1);
    }

    /** The key list of every node in a snapshot, in order. */
    private List<List<Integer>> nodeKeys(List<NodeDto> nodes) {
        List<List<Integer>> keys = new ArrayList<>();
        for (NodeDto node : nodes) {
            keys.add(node.getKeys());
        }
        return keys;
    }

    /** Tower heights per key, keys in sorted order. */
    private List<Integer> towerHeights(List<NodeDto> nodes) {
        TreeMap<Integer, Integer> heights = new TreeMap<>();
        for (NodeDto node : nodes) {
            heights.merge(node.getValue(), 1, Integer::sum);
        }
        return new ArrayList<>(heights.values());
    }

    /** Whether an edge from {@code from} to {@code to} exists. */
    private boolean hasEdge(List<EdgeDto> edges, int from, int to) {
        for (EdgeDto edge : edges) {
            if (edge.getFrom() == from && edge.getTo() == to) {
                return true;
            }
        }
        return false;
    }

    /** A request with only values set. */
    private TraceRequest valuesOnlyRequest(List<Integer> values) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        return request;
    }

    /** A request with values and a target set. */
    private TraceRequest searchRequest(List<Integer> values,
                                       Integer target) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        request.setTarget(target);
        return request;
    }

    /** A request with values and a value set. */
    private TraceRequest deleteRequest(List<Integer> values,
                                       Integer value) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        request.setValue(value);
        return request;
    }

    /** A request with only a word set. */
    private TraceRequest wordRequest(String word) {
        TraceRequest request = new TraceRequest();
        request.setWord(word);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the Phase 17 and Phase 18 ones (validation 400s before any
     * service is called). The parameter order is the
     * constructor's: 28 nulls, then lru, btree, bloom,
     * suffixArray, skipList (33 total). */
   
    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- B-tree search tests ----

    @Test
    void btreeSearchFound17() {
        List<Step> steps = bTreeService.search(DEMO_TREE, 17);
        // Visit root, visit leaf, found, final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Visit node [10, 20]: 17 is greater than 10, "
                        + "less than 20 — go to the middle child.",
                steps.get(0).getMessage());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals(2, steps.get(0).getLine());
        assertEquals("Visit node [12, 17]: 17 is greater than 12, "
                        + "equal to 17.",
                steps.get(1).getMessage());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals("Found 17 in node [12, 17] at position 1.",
                steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(2), steps.get(2).getHighlight());
        // The path is root [10, 20] then leaf [12, 17].
        assertEquals(List.of(List.of(5, 6, 7), List.of(10, 20),
                        List.of(12, 17), List.of(30)),
                nodeKeys(steps.get(0).getNodes()));
        assertEquals("Search complete: 17 found.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void btreeSearchNotFound15() {
        List<Step> steps = bTreeService.search(DEMO_TREE, 15);
        // Visit root, visit leaf, not found, final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Visit node [10, 20]: 15 is greater than 10, "
                        + "less than 20 — go to the middle child.",
                steps.get(0).getMessage());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals("Visit node [12, 17]: 15 is greater than 12, "
                        + "less than 17.",
                steps.get(1).getMessage());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals("15 is not in the tree (reached leaf "
                        + "[12, 17]).",
                steps.get(2).getMessage());
        assertEquals(2, steps.get(2).getLine());
        assertEquals(List.of(2), steps.get(2).getHighlight());
        assertEquals("Search complete: 15 not found.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void btreeSearchRootHit() {
        List<Step> steps = bTreeService.search(DEMO_TREE, 10);
        // Visit root, found, final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Visit node [10, 20]: 10 is equal to 10.",
                steps.get(0).getMessage());
        assertEquals("Found 10 in node [10, 20] at position 0.",
                steps.get(1).getMessage());
        assertEquals(3, steps.get(1).getLine());
        assertEquals(List.of(1), steps.get(1).getHighlight());
        assertEquals("Search complete: 10 found.",
                lastStep(steps).getMessage());
    }

    // ---- B-tree delete tests ----

    @Test
    void btreeDeleteLeaf6() {
        List<Step> steps = bTreeService.delete(DEMO_TREE, 6);
        // Leaf delete + final = 2 steps.
        assertEquals(2, steps.size());
        assertEquals("Delete 6 from leaf [5, 6, 7] → [5, 7].",
                steps.get(0).getMessage());
        assertEquals(2, steps.get(0).getLine());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals("Final tree: [10, 20] | [5, 7] [12, 17] [30].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(List.of(5, 7), List.of(10, 20),
                        List.of(12, 17), List.of(30)),
                nodeKeys(lastStep(steps).getNodes()));
    }

    @Test
    void btreeDeleteInternal20() {
        List<Step> steps = bTreeService.delete(DEMO_TREE, 20);
        // Predecessor replacement + leaf delete + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Delete 20 from root [10, 20]: replaced by "
                        + "predecessor 17.",
                steps.get(0).getMessage());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals("Delete 17 from leaf [12, 17] → [12].",
                steps.get(1).getMessage());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals("Final tree: [10, 17] | [5, 6, 7] [12] [30].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeDeleteBorrow30() {
        List<Step> steps = bTreeService.delete(DEMO_TREE, 30);
        // Borrow + leaf delete + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Leaf [30] has only 1 key — borrowing from "
                        + "left sibling [12, 17]: 17 moves up, 20 "
                        + "moves down.",
                steps.get(0).getMessage());
        assertEquals(List.of(3), steps.get(0).getHighlight());
        assertEquals("Delete 30 from leaf [20, 30] → [20].",
                steps.get(1).getMessage());
        assertEquals(List.of(3), steps.get(1).getHighlight());
        assertEquals("Final tree: [10, 17] | [5, 6, 7] [12] [20].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeDeleteLeaf12() {
        List<Step> steps = bTreeService.delete(DEMO_TREE, 12);
        // Leaf delete + final = 2 steps.
        assertEquals(2, steps.size());
        assertEquals("Delete 12 from leaf [12, 17] → [17].",
                steps.get(0).getMessage());
        assertEquals("Final tree: [10, 20] | [5, 6, 7] [17] [30].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeDeleteMerge10() {
        List<Step> steps = bTreeService.delete(
                List.of(10, 20, 5, 6, 7, 3), 10);
        // Merge + leaf delete + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Delete 10 from root [6, 10]: both children "
                        + "have 1 key — merging [7], 10 and [20] "
                        + "into [7, 10, 20].",
                steps.get(0).getMessage());
        assertEquals(List.of(1), steps.get(0).getHighlight());
        assertEquals("Delete 10 from leaf [7, 10, 20] → [7, 20].",
                steps.get(1).getMessage());
        assertEquals(List.of(3), steps.get(1).getHighlight());
        assertEquals("Final tree: [6] | [3, 5] [7, 20].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeDeleteBorrowSmallTree() {
        List<Step> steps = bTreeService.delete(
                List.of(10, 20, 5, 6), 20);
        // Borrow + leaf delete + final = 3 steps.
        assertEquals(3, steps.size());
        assertEquals("Leaf [20] has only 1 key — borrowing from "
                        + "left sibling [5, 6]: 6 moves up, 10 "
                        + "moves down.",
                steps.get(0).getMessage());
        assertEquals(List.of(2), steps.get(0).getHighlight());
        assertEquals("Delete 20 from leaf [10, 20] → [10].",
                steps.get(1).getMessage());
        assertEquals(List.of(2), steps.get(1).getHighlight());
        assertEquals("Final tree: [6] | [5] [10].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeDeleteNotPresent() {
        List<Step> steps = bTreeService.delete(DEMO_TREE, 15);
        // Not present + final = 2 steps; the tree is unchanged.
        assertEquals(2, steps.size());
        assertEquals("Key 15 is not in the tree.",
                steps.get(0).getMessage());
        assertEquals(2, steps.get(0).getLine());
        assertEquals("Final tree: [10, 20] | [5, 6, 7] [12, 17] "
                        + "[30].",
                lastStep(steps).getMessage());
    }

    // ---- Suffix array tests ----

    @Test
    void suffixArrayBanana() {
        List<Step> steps = suffixArrayService.suffixArray("banana");
        // 6 rank steps + final = 7 steps.
        assertEquals(7, steps.size());
        assertEquals("Rank 0: suffix 5 'a'.",
                steps.get(0).getMessage());
        assertEquals(List.of(5), steps.get(0).getData());
        assertEquals(List.of(5), steps.get(0).getHighlight());
        assertNull(steps.get(0).getAux());
        assertEquals("Rank 1: suffix 3 'ana'.",
                steps.get(1).getMessage());
        assertEquals(List.of(5, 3), steps.get(1).getData());
        assertEquals("Rank 2: suffix 1 'anana'.",
                steps.get(2).getMessage());
        assertEquals("Rank 3: suffix 0 'banana'.",
                steps.get(3).getMessage());
        assertEquals("Rank 4: suffix 4 'na'.",
                steps.get(4).getMessage());
        assertEquals("Rank 5: suffix 2 'nana'.",
                steps.get(5).getMessage());
        assertEquals(List.of(5, 3, 1, 0, 4, 2),
                steps.get(5).getData());
        assertEquals(List.of(5, 3, 1, 0, 4, 2),
                lastStep(steps).getData());
        assertEquals("Suffix array: [5, 3, 1, 0, 4, 2]; "
                        + "LCP: [1, 3, 0, 0, 2].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
        assertEquals(List.of(1, 3, 0, 0, 2),
                lastStep(steps).getAux());
    }

    @Test
    void suffixArraySingleChar() {
        List<Step> steps = suffixArrayService.suffixArray("a");
        // 1 rank step + final = 2 steps; no LCP pairs.
        assertEquals(2, steps.size());
        assertEquals("Rank 0: suffix 0 'a'.",
                steps.get(0).getMessage());
        assertEquals("Suffix array: [0]; LCP: [].",
                lastStep(steps).getMessage());
        assertEquals(List.of(), lastStep(steps).getAux());
    }

    // ---- Skip list tests ----

    @Test
    void skipListDemo() {
        List<Step> steps = skipListService.insert(
                List.of(3, 6, 7, 9, 12));
        // 5 inserts + final = 6 steps.
        assertEquals(6, steps.size());
        assertEquals("Insert 3 (level 1): first key; tower of "
                        + "height 1.",
                steps.get(0).getMessage());
        assertEquals(List.of(0), steps.get(0).getHighlight());
        assertEquals("Insert 6 (level 1): linked after 3 at "
                        + "level 1.",
                steps.get(1).getMessage());
        assertEquals("Insert 7 (level 3): linked after 6 at "
                        + "level 1; levels 2–3 start at the header.",
                steps.get(2).getMessage());
        assertEquals(List.of(2, 3, 4), steps.get(2).getHighlight());
        assertEquals("Insert 9 (level 1): linked after 7 at "
                        + "level 1.",
                steps.get(3).getMessage());
        assertEquals("Insert 12 (level 4): linked after 7 at "
                        + "levels 2–3, after 9 at level 1; level 4 "
                        + "starts at the header.",
                steps.get(4).getMessage());
        assertEquals(List.of(6, 7, 8, 9),
                steps.get(4).getHighlight());
        // 10 tower cells: heights 1, 1, 3, 1, 4 in sorted order.
        assertEquals(10, lastStep(steps).getNodes().size());
        assertEquals(List.of(1, 1, 3, 1, 4),
                towerHeights(lastStep(steps).getNodes()));
        // 5 vertical + 6 horizontal edges = 11.
        assertEquals(11, lastStep(steps).getEdges().size());
        assertTrue(hasEdge(lastStep(steps).getEdges(), 2, 3));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 3, 4));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 6, 7));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 0, 1));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 1, 2));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 2, 5));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 5, 6));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 3, 7));
        assertTrue(hasEdge(lastStep(steps).getEdges(), 4, 8));
        assertEquals("Skip list (max level 4): 5 keys; tower "
                        + "heights 1, 1, 3, 1, 4.",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void skipListTenKeys() {
        List<Step> steps = skipListService.insert(
                List.of(3, 6, 7, 9, 12, 19, 17, 26, 21, 25));
        // 10 inserts + final = 11 steps; 20 tower cells.
        assertEquals(11, steps.size());
        assertEquals(20, lastStep(steps).getNodes().size());
        assertEquals(List.of(1, 1, 3, 1, 4, 2, 1, 2, 2, 3),
                towerHeights(lastStep(steps).getNodes()));
        assertEquals("Skip list (max level 4): 10 keys; tower "
                        + "heights 1, 1, 3, 1, 4, 2, 1, 2, 2, 3.",
                lastStep(steps).getMessage());
    }

    @Test
    void skipListDuplicateSkipped() {
        List<Step> steps = skipListService.insert(
                List.of(3, 6, 3, 7));
        // The duplicate 3 draws no level and emits no step:
        // 3 inserts + final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals(List.of(1, 1, 3),
                towerHeights(lastStep(steps).getNodes()));
        assertEquals("Skip list (max level 3): 3 keys; tower "
                        + "heights 1, 1, 3.",
                lastStep(steps).getMessage());
    }

    // ---- 400 tests (controller validation) ----

  
}
