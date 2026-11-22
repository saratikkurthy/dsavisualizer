package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.model.NodeDto;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.service.BucketSortService;
import com.sarat.dsavisualizer.service.DpService;
import com.sarat.dsavisualizer.service.RabinKarpService;
import com.sarat.dsavisualizer.service.TrieService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 15 service tests. Bucket-sort tests check the bucket
 * snapshot after placement and the sorted array; Rabin-Karp tests
 * check the pattern hash, every window hash in order, the
 * spurious-hit reject step at window 6 and the hit at index 10;
 * LIS tests check the dp array and the reconstructed subsequence
 * (the traceback starts from the LAST index with the max dp, so
 * [2, 5, 7, 18] — not [2, 3, 7, 18]); MCM tests check the
 * minimum cost and the parenthesization for three dimension
 * arrays; trie-delete tests check the word-end ids and the pruned
 * nodes in the final step.
 */
class Phase15ServiceTest {

    private final BucketSortService bucketSortService =
            new BucketSortService();
    private final RabinKarpService rabinKarpService =
            new RabinKarpService();
    private final DpService dpService = new DpService();
    private final TrieService trieService = new TrieService();

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

    /** Last step whose message starts with the given prefix. */
    private Step lastStepWithPrefix(List<Step> steps, String prefix) {
        Step found = null;
        for (Step step : steps) {
            if (step.getMessage().startsWith(prefix)) {
                found = step;
            }
        }
        if (found == null) {
            throw new IllegalStateException(
                    "no step starts with: " + prefix);
        }
        return found;
    }

    // ---- Bucket sort tests ----

    @Test
    void bucketSortExample() {
        // max 49 -> 5 buckets; 29 goes in bucket 2 (29//10), not 3.
        List<Step> steps = bucketSortService.bucketSort(
                List.of(29, 25, 3, 49, 9, 37, 21, 43));
        Step lastPlacement = lastStepWithPrefix(steps, "Put ");
        assertEquals(List.of(List.of(3, 9), List.of(),
                        List.of(29, 25, 21), List.of(37),
                        List.of(49, 43)),
                lastPlacement.getBuckets());
        // Each non-empty bucket is sorted in place.
        assertTrue(anyMessageContains(steps,
                "Sorted bucket 2: [21, 25, 29]."));
        assertTrue(anyMessageContains(steps,
                "Sorted bucket 4: [43, 49]."));
        Step last = lastStep(steps);
        assertEquals(List.of(3, 9, 21, 25, 29, 37, 43, 49),
                last.getData());
        assertTrue(last.getMessage().contains(
                "Sorted: [3, 9, 21, 25, 29, 37, 43, 49]."));
    }

    @Test
    void bucketSortSecond() {
        // max 19 -> 2 buckets.
        List<Step> steps = bucketSortService.bucketSort(
                List.of(5, 12, 7, 19, 3));
        Step lastPlacement = lastStepWithPrefix(steps, "Put ");
        assertEquals(List.of(List.of(5, 7, 3), List.of(12, 19)),
                lastPlacement.getBuckets());
        assertTrue(anyMessageContains(steps,
                "Sorted bucket 0: [3, 5, 7]."));
        Step last = lastStep(steps);
        assertEquals(List.of(3, 5, 7, 12, 19), last.getData());
        assertTrue(last.getMessage().contains(
                "Sorted: [3, 5, 7, 12, 19]."));
    }

    // ---- Rabin-Karp tests ----

    /** Window steps in order: messages starting with "Window " or
     * "Pattern hash: ... Window " (the first step states the
     * pattern hash and is also the window-0 step). The spurious
     * reject step says "window 6" in lower case, so it is not
     * collected here. */
    private List<Integer> windowHashes(List<Step> steps) {
        List<Integer> hashes = new ArrayList<>();
        for (Step step : steps) {
            if (step.getMessage().contains("Window ")) {
                hashes.add(step.getData().get(1));
            }
        }
        return hashes;
    }

    @Test
    void rabinKarpExample() {
        List<Step> steps = rabinKarpService.rabinKarp(
                List.of("ABABDABACDABABCABAB", "ABABCABAB"));
        // The first step states the pattern hash.
        assertTrue(steps.get(0).getMessage()
                .contains("Pattern hash: 88."));
        // Every window hash, in order.
        assertEquals(List.of(56, 17, 5, 89, 55, 91, 88, 1, 75, 83, 88),
                windowHashes(steps));
        // Window 6 is a genuine spurious hit: hash 88, but the
        // text "BACDABABC" is not the pattern — the trace must show
        // the reject step and keep scanning.
        assertTrue(anyMessageContains(steps,
                "Window 6: hash 88 = pattern hash 88. "
                        + "Checking characters…"));
        assertTrue(anyMessageContains(steps,
                "Hash matches at window 6, but the text differs — "
                        + "spurious hit, keep scanning."));
        assertTrue(lastStep(steps).getMessage()
                .contains("Pattern found at index 10."));
    }

    @Test
    void rabinKarpNoMatch() {
        List<Step> steps = rabinKarpService.rabinKarp(
                List.of("AAAA", "AAAB"));
        assertTrue(steps.get(0).getMessage()
                .contains("Pattern hash: 26."));
        assertEquals(List.of(25), windowHashes(steps));
        assertTrue(lastStep(steps).getMessage()
                .contains("Pattern not found."));
    }

    // ---- LIS tests ----

    @Test
    void lisExample() {
        List<Step> steps = dpService.lis(
                List.of(10, 9, 2, 5, 3, 7, 101, 18));
        Step last = lastStep(steps);
        assertEquals(List.of(1, 1, 1, 2, 2, 3, 4, 4),
                last.getData());
        assertTrue(last.getMessage().contains(
                "Longest increasing subsequence has length 4: "
                        + "[2, 5, 7, 18]."));
        // The per-i messages name the best previous value and its
        // index: dp[5] = 3 extends the 5 at index 3 (first j wins
        // the tie with index 4) — not "7 at index 3".
        assertTrue(anyMessageContains(steps,
                "dp[5] = 3 (best previous: 5 at index 3)."));
        assertTrue(anyMessageContains(steps,
                "dp[7] = 4 (best previous: 7 at index 5)."));
        // dp[3] extends the 2 at index 2.
        assertTrue(anyMessageContains(steps,
                "dp[3] = 2 (best previous: 2 at index 2)."));
    }

    @Test
    void lisSecond() {
        List<Step> steps = dpService.lis(List.of(3, 10, 2, 1, 20));
        Step last = lastStep(steps);
        assertEquals(List.of(1, 2, 1, 1, 3), last.getData());
        assertTrue(last.getMessage().contains(
                "Longest increasing subsequence has length 3: "
                        + "[3, 10, 20]."));
    }

    // ---- MCM tests ----

    @Test
    void mcmExample() {
        List<Step> steps = dpService.mcm(List.of(1, 2, 3, 4));
        // The chain-length-2 step fills m[1][2] = 6, m[2][3] = 24.
        assertEquals(6, steps.get(0).getTable().get(1).get(2));
        assertEquals(24, steps.get(0).getTable().get(2).get(3));
        assertTrue(steps.get(0).getMessage().contains(
                "Chain length 2: m[1][2] = 6 (k = 1), "
                        + "m[2][3] = 24 (k = 2)."));
        Step last = lastStep(steps);
        assertEquals(18, last.getTable().get(1).get(3));
        assertTrue(last.getMessage()
                .contains("Minimum cost 18. ((A1A2)A3)."));
    }

    @Test
    void mcmTwoMatrices() {
        List<Step> steps = dpService.mcm(List.of(10, 20, 30));
        Step last = lastStep(steps);
        assertEquals(6000, last.getTable().get(1).get(2));
        assertTrue(last.getMessage()
                .contains("Minimum cost 6000. (A1A2)."));
    }

    @Test
    void mcmClassic() {
        // The CLRS example: 26000, ((A1(A2A3))A4).
        List<Step> steps = dpService.mcm(List.of(40, 20, 30, 10, 30));
        Step last = lastStep(steps);
        assertEquals(26000, last.getTable().get(1).get(4));
        assertTrue(last.getMessage()
                .contains("Minimum cost 26000. ((A1(A2A3))A4)."));
    }

    // ---- Trie delete tests ----

    /** Word-end node ids in the final step's trie. */
    private List<Integer> wordEndIds(List<Step> steps) {
        return lastStep(steps).getSorted();
    }

    /** Node ids in the final step's trie. */
    private Set<Integer> nodeIds(List<Step> steps) {
        Set<Integer> ids = new HashSet<>();
        for (NodeDto node : lastStep(steps).getNodes()) {
            ids.add(node.getId());
        }
        return ids;
    }

    @Test
    void trieDeleteCar() {
        // cat, car, card: node ids are root 0, c 1, a 2, t 3, r 4,
        // d 5. Deleting "car" unmarks r (id 4) but cannot prune it
        // — "card" still needs it.
        List<Step> steps = trieService.delete(
                List.of("cat", "car", "card"), "car");
        assertTrue(lastStep(steps).getMessage()
                .contains("Deleted 'car'."));
        assertEquals(List.of(3, 5), wordEndIds(steps));
        assertEquals(Set.of(0, 1, 2, 3, 4, 5), nodeIds(steps));
        assertTrue(anyMessageContains(steps,
                "Unmark 'car' as a word."));
    }

    @Test
    void trieDeleteCat() {
        // Deleting "cat" unmarks t (id 3) and prunes it — t has no
        // children — but stops at a (id 2), which still has child r.
        List<Step> steps = trieService.delete(
                List.of("cat", "car", "card"), "cat");
        assertTrue(lastStep(steps).getMessage()
                .contains("Deleted 'cat'."));
        assertEquals(List.of(4, 5), wordEndIds(steps));
        assertEquals(Set.of(0, 1, 2, 4, 5), nodeIds(steps));
        assertTrue(anyMessageContains(steps,
                "Prune node 't' — no children, ends no word."));
    }

    @Test
    void trieDeleteDog() {
        // "dog" is absent — the path breaks at 'd' — so nothing
        // changes and every word end stays.
        List<Step> steps = trieService.delete(
                List.of("cat", "car", "card"), "dog");
        assertTrue(lastStep(steps).getMessage()
                .contains("Word 'dog' not found."));
        assertEquals(List.of(3, 4, 5), wordEndIds(steps));
        assertEquals(Set.of(0, 1, 2, 3, 4, 5), nodeIds(steps));
    }

    @Test
    void trieDeleteCarAgain() {
        // After "car" is gone (only "cat" and "card" left), the
        // path c-a-r still exists but ends no word — deleting
        // "car" again is a miss.
        List<Step> steps = trieService.delete(
                List.of("cat", "card"), "car");
        assertTrue(lastStep(steps).getMessage()
                .contains("Word 'car' not found."));
        assertEquals(List.of(3, 5), wordEndIds(steps));
    }
}
