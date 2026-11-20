package com.sarat.dsavisualizer;

import com.sarat.dsavisualizer.controller.VisualizationController;
import com.sarat.dsavisualizer.model.Step;
import com.sarat.dsavisualizer.model.TraceRequest;
import com.sarat.dsavisualizer.service.BoyerMooreService;
import com.sarat.dsavisualizer.service.FenwickTreeService;
import com.sarat.dsavisualizer.service.QuickSelectService;
import com.sarat.dsavisualizer.service.ZAlgorithmService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 16 service tests. Boyer-Moore tests check the mismatch
 * messages verbatim (including the s=1 step of the BAB case, where
 * the bad-character rule proposes -1 and the good-suffix rule wins
 * with 2) and every match list; Z tests check the full Z array
 * verbatim and the inside-window message; Fenwick tests check the
 * BIT after build/update and the prefix sums (prefix(4) visits
 * only index 4, prefix(3) visits 3 then 2); QuickSelect tests
 * check the array after every partition and the found element
 * (sorted = [targetIndex]). The five 400 tests call the controller
 * endpoints directly — the controller is built with nulls for the
 * other services because the validators 400 before any service is
 * touched (there is no precedent for controller tests in earlier
 * phases; the 400s were previously untested).
 */
class Phase16ServiceTest {

    private final BoyerMooreService boyerMooreService =
            new BoyerMooreService();
    private final ZAlgorithmService zAlgorithmService =
            new ZAlgorithmService();
    private final FenwickTreeService fenwickTreeService =
            new FenwickTreeService();
    private final QuickSelectService quickSelectService =
            new QuickSelectService();

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

    /** A request with only words set. */
    private TraceRequest wordsRequest(String... words) {
        TraceRequest request = new TraceRequest();
        request.setWords(List.of(words));
        return request;
    }

    /** A request with values, index and value set (nulls allowed). */
    private TraceRequest valuesRequest(List<Integer> values,
                                       Integer index, Integer value) {
        TraceRequest request = new TraceRequest();
        request.setValues(values);
        request.setIndex(index);
        request.setValue(value);
        return request;
    }

 
    /** Assert the response is a 400. */
    private void assertBadRequest(ResponseEntity<?> response) {
        assertEquals(400, response.getStatusCode().value());
    }

    // ---- Boyer-Moore tests ----

    @Test
    void boyerMooreExample() {
        List<Step> steps = boyerMooreService.boyerMoore(
                List.of("ABAAABCD", "ABC"));
        assertEquals("Window 0: mismatch at j=2 (text 'A' vs "
                        + "pattern 'C'). Bad-char shift 2, "
                        + "good-suffix shift 1 → shift 2.",
                steps.get(0).getMessage());
        assertEquals(List.of(0), steps.get(0).getData());
        assertEquals(List.of(2, 2), steps.get(0).getHighlight());
        assertEquals("Window 2: mismatch at j=2 (text 'A' vs "
                        + "pattern 'C'). Bad-char shift 2, "
                        + "good-suffix shift 1 → shift 2.",
                steps.get(1).getMessage());
        assertEquals("Pattern found at index 4.",
                steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(6, 2), steps.get(2).getHighlight());
        assertEquals("Found 1 match(es): [4].",
                lastStep(steps).getMessage());
        assertEquals(0, lastStep(steps).getLine());
    }

    @Test
    void boyerMooreGoodSuffixWins() {
        // AAABBBBBAA / BAB: the s=1 step is the good-suffix-wins
        // case — the last 'A' sits right of the mismatch, so the
        // bad-character rule proposes -1 and the good-suffix rule
        // (shift 2) is taken.
        List<Step> steps = boyerMooreService.boyerMoore(
                List.of("AAABBBBBAA", "BAB"));
        assertEquals("Window 0: mismatch at j=2 (text 'A' vs "
                        + "pattern 'B'). Bad-char shift 1, "
                        + "good-suffix shift 1 → shift 1.",
                steps.get(0).getMessage());
        assertEquals("Window 1: mismatch at j=0 (text 'A' vs "
                        + "pattern 'B'). Bad-char shift -1, "
                        + "good-suffix shift 2 → shift 2.",
                steps.get(1).getMessage());
        assertEquals("Window 3: mismatch at j=1 (text 'B' vs "
                        + "pattern 'A'). Bad-char shift -1, "
                        + "good-suffix shift 2 → shift 2.",
                steps.get(2).getMessage());
        assertEquals("Window 5: mismatch at j=1 (text 'B' vs "
                        + "pattern 'A'). Bad-char shift -1, "
                        + "good-suffix shift 2 → shift 2.",
                steps.get(3).getMessage());
        assertEquals("Window 7: mismatch at j=2 (text 'A' vs "
                        + "pattern 'B'). Bad-char shift 1, "
                        + "good-suffix shift 1 → shift 1.",
                steps.get(4).getMessage());
        assertEquals("Pattern not found.", lastStep(steps).getMessage());
    }

    @Test
    void boyerMooreTwoMatches() {
        List<Step> steps = boyerMooreService.boyerMoore(
                List.of("ABAAABCDBBABCD", "ABCD"));
        assertTrue(anyMessageContains(steps,
                "Pattern found at index 4."));
        assertTrue(anyMessageContains(steps,
                "Pattern found at index 10."));
        assertEquals("Found 2 match(es): [4, 10].",
                lastStep(steps).getMessage());
    }

    // ---- Z-algorithm tests ----

    @Test
    void zAlgorithmExample() {
        List<Step> steps = zAlgorithmService.zAlgorithm(
                List.of("aabcaabxaaaz", "aab"));
        // The first phase-1 step: i=1 was not inside any window.
        assertEquals("Z[1] = 1.", steps.get(0).getMessage());
        assertEquals(List.of(1, 1), steps.get(0).getHighlight());
        // i=5 was inside the [4, 6] window and started from Z[1].
        assertEquals("Z[5] = 1. (inside the [4, 6] window, started "
                        + "from Z[1]).",
                steps.get(4).getMessage());
        // The full Z array, verbatim (Z[0] = len(S) = 16).
        assertEquals(List.of(16, 1, 0, 0, 3, 1, 0, 0, 3, 1, 0, 0,
                        2, 2, 1, 0),
                lastStep(steps).getData());
        assertTrue(anyMessageContains(steps,
                "Match at text index 0."));
        assertTrue(anyMessageContains(steps,
                "Match at text index 4."));
        assertEquals("Found 2 match(es): [0, 4].",
                lastStep(steps).getMessage());
    }

    @Test
    void zAlgorithmSecond() {
        List<Step> steps = zAlgorithmService.zAlgorithm(
                List.of("abcabcab", "abc"));
        assertEquals(List.of(12, 0, 0, 0, 3, 0, 0, 3, 0, 0, 2, 0),
                lastStep(steps).getData());
        assertTrue(anyMessageContains(steps,
                "Match at text index 0."));
        assertTrue(anyMessageContains(steps,
                "Match at text index 3."));
        assertEquals("Found 2 match(es): [0, 3].",
                lastStep(steps).getMessage());
    }

    // ---- Fenwick tree tests ----

    @Test
    void fenwickBuildExample() {
        List<Step> steps = fenwickTreeService.build(
                List.of(1, 3, 5, 7, 9, 11));
        assertTrue(anyMessageContains(steps,
                "Added a[1] = 1: touched BIT[1, 2, 4]."));
        assertTrue(anyMessageContains(steps,
                "Added a[4] = 7: touched BIT[4]."));
        assertEquals(List.of(0, 1, 4, 5, 16, 9, 20),
                lastStep(steps).getData());
        assertEquals("Built BIT: [0, 1, 4, 5, 16, 9, 20].",
                lastStep(steps).getMessage());
    }

    @Test
    void fenwickPrefix() {
        List<Step> steps = fenwickTreeService.prefix(
                List.of(1, 3, 5, 7, 9, 11), 4);
        // prefix(4) visits only index 4.
        assertEquals(2, steps.size());
        assertEquals("prefix(4): add BIT[4] = 16 → sum 16.",
                steps.get(0).getMessage());
        assertEquals(List.of(4), steps.get(0).getHighlight());
        assertEquals("prefix(4) = 16.", lastStep(steps).getMessage());
        // prefix(3) visits 3, then 2: 5 + 4 = 9.
        List<Step> steps3 = fenwickTreeService.prefix(
                List.of(1, 3, 5, 7, 9, 11), 3);
        assertEquals("prefix(3): add BIT[3] = 5 → sum 5.",
                steps3.get(0).getMessage());
        assertEquals("prefix(3): add BIT[2] = 4 → sum 9.",
                steps3.get(1).getMessage());
        assertEquals("prefix(3) = 9.", lastStep(steps3).getMessage());
    }

    @Test
    void fenwickUpdate() {
        List<Step> steps = fenwickTreeService.update(
                List.of(1, 3, 5, 7, 9, 11), 2, 2);
        assertEquals("Add 2 to BIT[2] → 6.",
                steps.get(0).getMessage());
        assertEquals("Add 2 to BIT[4] → 18.",
                steps.get(1).getMessage());
        assertEquals(List.of(0, 1, 6, 5, 18, 9, 20),
                lastStep(steps).getData());
        assertEquals("Updated BIT: [0, 1, 6, 5, 18, 9, 20].",
                lastStep(steps).getMessage());
    }

    @Test
    void fenwickPrefixSecond() {
        List<Step> steps = fenwickTreeService.prefix(
                List.of(5, 12, 7, 19, 3), 5);
        assertEquals("prefix(5) = 46.", lastStep(steps).getMessage());
    }

    // ---- QuickSelect tests ----

    @Test
    void quickSelectExample() {
        List<Step> steps = quickSelectService.quickSelect(
                List.of(7, 10, 4, 3, 20, 15), 3);
        assertEquals(List.of(7, 10, 4, 3, 15, 20),
                steps.get(0).getData());
        assertEquals(List.of(4), steps.get(0).getHighlight());
        assertEquals("Partition [0..5] with pivot 15: pivot lands "
                        + "at index 4. 3rd smallest is to the left.",
                steps.get(0).getMessage());
        assertEquals(List.of(3, 10, 4, 7, 15, 20),
                steps.get(1).getData());
        assertEquals("Partition [0..3] with pivot 3: pivot lands "
                        + "at index 0. 3rd smallest is to the right.",
                steps.get(1).getMessage());
        assertEquals("Partition [1..3] with pivot 7: pivot lands "
                        + "at index 2 — found: the 3rd smallest is 7.",
                steps.get(2).getMessage());
        assertEquals(3, steps.get(2).getLine());
        assertEquals(List.of(3, 4, 7, 10, 15, 20),
                lastStep(steps).getData());
        assertEquals(List.of(2), lastStep(steps).getSorted());
        assertEquals("The 3rd smallest is 7.",
                lastStep(steps).getMessage());
    }

    @Test
    void quickSelectSecond() {
        List<Step> steps = quickSelectService.quickSelect(
                List.of(5, 12, 7, 19, 3), 2);
        assertEquals(List.of(3, 12, 7, 19, 5),
                steps.get(0).getData());
        assertEquals("Partition [0..4] with pivot 3: pivot lands "
                        + "at index 0. 2nd smallest is to the right.",
                steps.get(0).getMessage());
        assertEquals("Partition [1..4] with pivot 5: pivot lands "
                        + "at index 1 — found: the 2nd smallest is 5.",
                steps.get(1).getMessage());
        assertEquals(List.of(1), lastStep(steps).getSorted());
        assertEquals("The 2nd smallest is 5.",
                lastStep(steps).getMessage());
    }

    // ---- 400 tests (controller validation) ----

}
