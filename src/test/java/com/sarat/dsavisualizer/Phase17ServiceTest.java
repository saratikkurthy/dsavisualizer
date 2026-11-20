package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.BloomFilterService;
import com.sarat.dsavisualizer.service.BTreeService;
import com.sarat.dsavisualizer.service.LruCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 17 service tests. LRU tests check the cache list after
 * every op of the verified demo (including the evictions of 2
 * and 3, the get(2) miss and the final [4, 5, 1]), the
 * put-update message and the capacity-1 eviction. B-tree tests
 * check the level-order final string, the two split messages
 * verbatim (root split around median 10, the [12, 20, 30] split
 * around median 20), the node key lists after the root split
 * and the 11-step count. Bloom tests check the hash values
 * verbatim (apple h1 = 2, h2 = 10; banana h1 = 1, h2 = 14),
 * the bit arrays, the peach false positive and the grape
 * definitely-absent verdict. The four 400 tests call the
 * controller endpoints directly — the controller is built with
 * nulls for the other services because the validators 400
 * before any service is touched (the Phase 16 precedent).
 */
class Phase17ServiceTest {

    private final LruCacheService lruCacheService = new LruCacheService();
    private final BTreeService bTreeService = new BTreeService();
    private final BloomFilterService bloomFilterService =
            new BloomFilterService();

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

    /** A request with only capacity and words set. */
    private TraceRequest lruRequest(Integer capacity,
                                    String... words) {
        TraceRequest request = new TraceRequest();
        request.setCapacity(capacity);
        request.setWords(List.of(words));
        return request;
    }

    /** A request with only values set. */
    private TraceRequest valuesOnlyRequest(List<Integer> values) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        return request;
    }

    /** A request with words and word set. */
    private TraceRequest bloomRequest(List<String> words, String word) {
        TraceRequest request = new TraceRequest();
        request.setWords(words);
        request.setWord(word);
        return request;
    }

    /** The controller under test: nulls for every service except
     * the three Phase 17 ones (validation 400s before any service
     * is called). The parameter order is the constructor's. */
   

    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- LRU cache tests ----

    @Test
    void lruDemo() {
        List<Step> steps = lruCacheService.simulate(3, List.of(
                "put 1 10", "put 2 20", "put 3 30", "get 1",
                "put 4 40", "get 2", "put 5 50", "get 3", "get 4"));
        // 9 ops + final = 10 steps.
        assertEquals(10, steps.size());
        assertEquals(List.of(3, 2, 1), steps.get(2).getData());
        assertEquals("put(3, 30): inserted; cache [3, 2, 1].",
                steps.get(2).getMessage());
        assertEquals("get(1): HIT → 10; moved to front; cache "
                        + "[1, 3, 2].",
                steps.get(3).getMessage());
        assertEquals(3, steps.get(3).getLine());
        assertEquals(List.of(1, 3, 2), steps.get(3).getData());
        assertEquals("put(4, 40): inserted; evicted 2 (least "
                        + "recently used); cache [4, 1, 3].",
                steps.get(4).getMessage());
        assertEquals(List.of(4, 1, 3), steps.get(4).getData());
        assertEquals("get(2): MISS; cache unchanged [4, 1, 3].",
                steps.get(5).getMessage());
        assertEquals("put(5, 50): inserted; evicted 3 (least "
                        + "recently used); cache [5, 4, 1].",
                steps.get(6).getMessage());
        assertEquals(List.of(5, 4, 1), steps.get(6).getData());
        assertEquals("get(3): MISS; cache unchanged [5, 4, 1].",
                steps.get(7).getMessage());
        assertEquals("get(4): HIT → 40; moved to front; cache "
                        + "[4, 5, 1].",
                steps.get(8).getMessage());
        assertEquals(3, steps.get(8).getLine());
        assertEquals(List.of(4, 5, 1), lastStep(steps).getData());
        assertEquals("Final cache (most → least recent): [4, 5, 1].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void lruPutUpdate() {
        List<Step> steps = lruCacheService.simulate(3, List.of(
                "put 1 10", "put 2 20", "put 1 99"));
        assertEquals("put(1, 99): updated value; moved to front; "
                        + "cache [1, 2].",
                steps.get(2).getMessage());
        assertEquals(List.of(1, 2), steps.get(2).getData());
        assertEquals(List.of(1), steps.get(2).getHighlight());
        assertEquals("Final cache (most → least recent): [1, 2].",
                lastStep(steps).getMessage());
    }

    @Test
    void lruCapacityOne() {
        List<Step> steps = lruCacheService.simulate(1, List.of(
                "put 1 10", "put 2 20"));
        assertEquals("put(2, 20): inserted; evicted 1 (least "
                        + "recently used); cache [2].",
                steps.get(1).getMessage());
        assertEquals(List.of(2), lastStep(steps).getData());
        assertEquals("Final cache (most → least recent): [2].",
                lastStep(steps).getMessage());
    }

    @Test
    void lruGetMissEmptyCache() {
        List<Step> steps = lruCacheService.simulate(3,
                List.of("get 5"));
        assertEquals(2, steps.size());
        assertEquals("get(5): MISS; cache unchanged [].",
                steps.get(0).getMessage());
        assertEquals(List.of(), steps.get(0).getData());
        assertEquals("Final cache (most → least recent): [].",
                lastStep(steps).getMessage());
    }

    // ---- B-tree tests ----

    @Test
    void btreeDemo() {
        List<Step> steps = bTreeService.insert(
                List.of(10, 20, 5, 6, 12, 30, 7, 17));
        // 8 inserts + 2 splits + final = 11 steps.
        assertEquals(11, steps.size());
        assertEquals("Insert 10 → leaf [10].",
                steps.get(0).getMessage());
        assertEquals(List.of(List.of(10)),
                nodeKeys(steps.get(0).getNodes()));
        assertEquals("Insert 5 → leaf [5, 10, 20].",
                steps.get(2).getMessage());
        assertEquals(List.of(List.of(5, 10, 20)),
                nodeKeys(steps.get(2).getNodes()));
        // The root split: median 10 moves up.
        assertEquals("Root is full — split around median 10; new "
                        + "root [10], children [5] and [20].",
                steps.get(3).getMessage());
        assertEquals(List.of(List.of(5), List.of(10), List.of(20)),
                nodeKeys(steps.get(3).getNodes()));
        assertEquals("Insert 6 → leaf [5, 6].",
                steps.get(4).getMessage());
        assertEquals(List.of(List.of(5, 6), List.of(10),
                        List.of(20)),
                nodeKeys(steps.get(4).getNodes()));
        assertEquals("Insert 12 → leaf [12, 20].",
                steps.get(5).getMessage());
        assertEquals("Insert 30 → leaf [12, 20, 30].",
                steps.get(6).getMessage());
        assertEquals("Insert 7 → leaf [5, 6, 7].",
                steps.get(7).getMessage());
        // The [12, 20, 30] split: median 20 moves up.
        assertEquals("Split full node [12, 20, 30] around median "
                        + "20: left [12], right [30].",
                steps.get(8).getMessage());
        assertEquals(List.of(List.of(5, 6, 7), List.of(10, 20),
                        List.of(12), List.of(30)),
                nodeKeys(steps.get(8).getNodes()));
        assertEquals("Insert 17 → leaf [12, 17].",
                steps.get(9).getMessage());
        assertEquals(List.of(List.of(5, 6, 7), List.of(10, 20),
                        List.of(12, 17), List.of(30)),
                nodeKeys(steps.get(9).getNodes()));
        assertEquals("Final tree: [10, 20] | [5, 6, 7] [12, 17] "
                        + "[30].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void btreeRootSplitOnly() {
        List<Step> steps = bTreeService.insert(
                List.of(10, 20, 5, 6));
        // 4 inserts + 1 split + final = 6 steps.
        assertEquals(6, steps.size());
        assertTrue(steps.stream().anyMatch(step -> step.getMessage()
                .equals("Root is full — split around median 10; new "
                        + "root [10], children [5] and [20].")));
        assertEquals("Final tree: [10] | [5, 6] [20].",
                lastStep(steps).getMessage());
    }

    @Test
    void btreeSingleInsert() {
        List<Step> steps = bTreeService.insert(List.of(7));
        assertEquals(2, steps.size());
        assertEquals("Insert 7 → leaf [7].",
                steps.get(0).getMessage());
        assertEquals("Final tree: [7].", lastStep(steps).getMessage());
    }

    // ---- Bloom filter tests ----

    @Test
    void bloomAppleBananaPeach() {
        List<Step> steps = bloomFilterService.bloom(
                List.of("apple", "banana"), "peach");
        // 2 inserts + check + final = 4 steps.
        assertEquals(4, steps.size());
        assertEquals("Insert 'apple': h1 = 2, h2 = 10; bits set.",
                steps.get(0).getMessage());
        assertEquals(List.of(0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                        0, 0, 0),
                steps.get(0).getData());
        assertEquals(List.of(2, 10), steps.get(0).getHighlight());
        assertEquals("Insert 'banana': h1 = 1, h2 = 14; bits set.",
                steps.get(1).getMessage());
        assertEquals(List.of(0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0,
                        0, 1, 0),
                steps.get(1).getData());
        // peach: h1 = h2 = 1, both set by banana — a false
        // positive (peach was never inserted).
        assertEquals("Check 'peach': h1 = 1 (1), h2 = 1 (1) → "
                        + "possibly present. 'peach' was never "
                        + "inserted, so this is a false positive.",
                steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(1, 1), steps.get(2).getHighlight());
        assertEquals("Bloom filter: bits set [1, 2, 10, 14].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void bloomGrapeAbsent() {
        List<Step> steps = bloomFilterService.bloom(
                List.of("apple", "banana"), "grape");
        assertEquals("Check 'grape': h1 = 15 (0), h2 = 7 (0) → "
                        + "definitely not present.",
                steps.get(2).getMessage());
        assertEquals(2, steps.get(2).getLine());
        assertEquals(List.of(15, 7), steps.get(2).getHighlight());
        assertEquals("Bloom filter: bits set [1, 2, 10, 14].",
                lastStep(steps).getMessage());
    }

    @Test
    void bloomCheckInsertedWord() {
        List<Step> steps = bloomFilterService.bloom(
                List.of("apple", "banana"), "apple");
        // apple was inserted, so no false-positive suffix.
        assertEquals("Check 'apple': h1 = 2 (1), h2 = 10 (1) → "
                        + "possibly present.",
                steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
    }

    // ---- 400 tests (controller validation) ----

}
